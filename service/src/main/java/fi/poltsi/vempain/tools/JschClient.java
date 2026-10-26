package fi.poltsi.vempain.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * SFTP transfer of published files to the site server. {@link #connect} opens a {@link Connection} that is owned by the caller and closed by it, so several
 * publish tasks may transfer at the same time, each over a connection of its own; this bean only holds the configuration and the shared {@link JSch} with the
 * admin user's identity. The transfers are measured on the global Micrometer registry (Spring Boot binds the application registry to it):
 * {@code vempain.site_transfer.files{kind}}, {@code vempain.site_transfer.bytes}, {@code vempain.site_transfer.duration{result}} and
 * {@code vempain.site_transfer.connect.failures}.
 */
@Slf4j
@Setter
@Component
public class JschClient {

    public static final String TRANSFER_FILES = "vempain.site_transfer.files";
    public static final String TRANSFER_BYTES = "vempain.site_transfer.bytes";
    public static final String TRANSFER_DURATION = "vempain.site_transfer.duration";
    public static final String CONNECT_FAILURES = "vempain.site_transfer.connect.failures";

    private static final int SESSION_TIMEOUT = 26_000;
    private static final int CHANNEL_TIMEOUT = 25_000;
    private final JSch jsch;
    /** Private key files already added to {@link #jsch}: an identity is added once, not once per connection. */
    private final Set<String> identities = ConcurrentHashMap.newKeySet();
    @Value("${vempain.site.www-root}")
    private String siteWwwRoot;
    @Value("${vempain.admin.file.site-file-directory}")
    private String siteFileDirectory;
    @Value("${vempain.site.thumb-directory}")
    private String thumbSubDir;
    @Value("${vempain.site.image-size}")
    private int siteImageSize;

    @Autowired
    private ImageTools imageTools;

    public JschClient() {
        this.jsch = new JSch();
    }

    /**
     * Opens an SFTP connection to the site server. The caller owns it: close it when the transfer is done (it is {@link AutoCloseable}).
     *
     * @throws JSchException when the session or the SFTP channel cannot be opened (counted in {@code vempain.site_transfer.connect.failures})
     */
    public Connection connect(String siteAddress, int sitePort, String siteUser, String adminSshHomeDir, String adminSshPrivateKey) throws JSchException {
        var knownHostFile = adminSshHomeDir + File.separator + ".ssh" + File.separator + "known_hosts";
        Session session;
        // The known hosts and the identities live in the shared JSch instance
        synchronized (jsch) {
            log.debug("Adding known host file from: {}", knownHostFile);
            jsch.setKnownHosts(knownHostFile);
            session = jsch.getSession(siteUser, siteAddress, sitePort);
            // Disable this for the moment as Jsch does not handle ssh-ed25519
            session.setConfig("StrictHostKeyChecking", "no");
            if (!identities.contains(adminSshPrivateKey)) {
                log.debug("Adding identity from private key file: {}", adminSshPrivateKey);
                try {
                    jsch.addIdentity(adminSshPrivateKey);
                    identities.add(adminSshPrivateKey);
                } catch (JSchException e) {
                    log.error("Failed to load private key file {}. Make sure this exists", adminSshPrivateKey);
                    Metrics.counter(CONNECT_FAILURES)
                        .increment();
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load private key file");
                }
            }
        }

        try {
            session.connect(SESSION_TIMEOUT);
            var channel = session.openChannel("sftp");
            channel.connect(CHANNEL_TIMEOUT);
            return new Connection(session, (ChannelSftp) channel);
        } catch (JSchException e) {
            Metrics.counter(CONNECT_FAILURES)
                .increment();
            if (session.isConnected()) {
                session.disconnect();
            }
            throw e;
        }
    }

    /**
     * Counts one transferred file and its size; the size is the local file's, 0 when it cannot be read.
     */
    private static void countTransferred(String kind, String localPath) {
        Metrics.counter(TRANSFER_FILES, "kind", kind)
            .increment();
        var bytes = new File(localPath).length();
        if (bytes > 0) {
            Metrics.counter(TRANSFER_BYTES)
                .increment(bytes);
        }
    }

    /**
     * One SFTP connection to the site server, used by one transfer at a time and closed by the publish that opened it.
     */
    public class Connection implements AutoCloseable {

        private final Session session;
        private final ChannelSftp channelSftp;

        Connection(Session session, ChannelSftp channelSftp) {
            this.session = session;
            this.channelSftp = channelSftp;
        }

        public void transferFilesToSite(List<SiteFile> siteFiles, List<FileThumb> thumbList) throws SftpException {
            transferFilesToSite(siteFiles, thumbList, null);
        }

        /**
         * Transfers the site files and their thumbnails to the site server. Every site file is transferred together with its own thumbnails, and
         * {@code onFileTransferred} is told after each one, so that a publish task can report one step per file while the transfer is running. Thumbnails whose
         * site file is not in the list are transferred last.
         *
         * @param onFileTransferred called after a site file and its thumbnails have been transferred; may be null
         */
        public void transferFilesToSite(List<SiteFile> siteFiles, List<FileThumb> thumbList, Consumer<SiteFile> onFileTransferred) throws SftpException {
            var sample = Timer.start(Metrics.globalRegistry);
            var result = "failure";
            try {
                transferAll(siteFiles, thumbList, onFileTransferred);
                result = "success";
            } finally {
                sample.stop(Metrics.timer(TRANSFER_DURATION, "result", result));
            }
        }

        @Override
        public void close() {
            if (session != null
                && session.isConnected()) {
                session.disconnect();
            }
        }

        private void transferAll(List<SiteFile> siteFiles, List<FileThumb> thumbList, Consumer<SiteFile> onFileTransferred) throws SftpException {
            if (!siteDirectoryExists(siteWwwRoot)) {
                log.error("The site main directory {} does not exist, file transfer is aborted", siteWwwRoot);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Site configuration is not set up correctly");
            }

            var remainingThumbs = new ArrayList<>(thumbList);

            for (var siteFile : siteFiles) {
                var absolutePathConvertedFile = siteFileDirectory + File.separator + siteFile.getFileType().shortName + File.separator + siteFile.getFilePath()
                        + File.separator + siteFile.getFileName();
                log.debug("Transferring file {} to {}", absolutePathConvertedFile, siteWwwRoot);

                var targetSubDir = siteFile.getFileType().shortName + File.separator + siteFile.getFilePath();
                var targetDir = siteWwwRoot + File.separator + targetSubDir;

                // We remove the leading / from the subdir so that it can be later split correctly
                if (targetSubDir.startsWith(File.separator)) {
                    targetSubDir = targetSubDir.substring(1);
                }

                if (!siteDirectoryExists(targetDir)) {
                    log.debug("Creating site directory: {}", targetDir);
                    var tmpArray = targetSubDir.split("/");
                    var siteDirPaths = new LinkedList<>(Arrays.asList(tmpArray));
                    // We add as the last item the .thumb directory which will then contain the thumb files
                    siteDirPaths.add(thumbSubDir);
                    createRecursivelySiteDirectory(siteWwwRoot, siteDirPaths);
                } else {
                    log.debug("Site directory {} already exists", targetDir);
                }

                channelSftp.put(absolutePathConvertedFile, targetDir + File.separator + siteFile.getFileName());
                countTransferred("file", absolutePathConvertedFile);

                for (var iterator = remainingThumbs.iterator(); iterator.hasNext();) {
                    var fileThumb = iterator.next();
                    if (fileThumb.getParentId() == siteFile.getId()) {
                        transferThumb(fileThumb);
                        iterator.remove();
                    }
                }

                if (onFileTransferred != null) {
                    onFileTransferred.accept(siteFile);
                }
            }

            for (var fileThumb : remainingThumbs) {
                transferThumb(fileThumb);
            }
        }

        private void transferThumb(FileThumb fileThumb) throws SftpException {
            var absolutePathThumbFile = siteFileDirectory + File.separator + fileThumb.getFilepath() + File.separator + fileThumb.getFilename();
            var targetDir = siteWwwRoot + File.separator + fileThumb.getParentType().shortName + File.separator + fileThumb.getSiteFile()
                .getFilePath() + File.separator + thumbSubDir;
            log.debug("Transferring thumb {} to {}", absolutePathThumbFile, targetDir);
            channelSftp.put(absolutePathThumbFile, targetDir);
            countTransferred("thumb", absolutePathThumbFile);
        }

        private void createRecursivelySiteDirectory(String mainDir, List<String> subDirList) throws SftpException {
            log.debug("Called with main directory {} and list of directory elements {}", mainDir, subDirList);

            if (subDirList.isEmpty()) {
                log.debug("No more directory elements to create");
                return;
            }

            String testDir = mainDir + File.separator + subDirList.getFirst();

            if (!siteDirectoryExists(testDir)) {
                log.debug("Element dir {} did not exist as {}, creating it", subDirList.getFirst(), testDir);
                channelSftp.mkdir(testDir);
            }

            log.debug("Popping first element from subdir list");
            subDirList.removeFirst();
            createRecursivelySiteDirectory(testDir, subDirList);
        }

        private boolean siteDirectoryExists(String siteDirectory) {
            try {
                channelSftp.lstat(siteDirectory);
                return true;
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    log.debug("Site directory {} does not exist", siteDirectory);
                } else {
                    // something else went wrong
                    log.error("Unknown error when checking site directory {}", siteDirectory, e);
                }
            }

            return false;
        }
    }
}
