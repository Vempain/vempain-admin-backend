package fi.poltsi.vempain.tools;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Setter
@Component
public class JschClient {
	private final        JSch        jsch;
	private              Session     jschSession;
	private              Channel     channel;
	private              ChannelSftp channelSftp;
	private static final int         SESSION_TIMEOUT = 26_000;
	private static final int         CHANNEL_TIMEOUT = 25_000;

	@Value("${vempain.site.www-root}")
	private String siteWwwRoot;
	@Value("${vempain.admin.file.site-file-directory}")
	private String siteFileDirectory;
	@Value("${vempain.site.thumb-directory}")
	private String thumbSubDir;
	@Value("${vempain.site.image-size}")
	private int    siteImageSize;

	@Autowired
	private ImageTools imageTools;

	public JschClient() {
		this.jsch = new JSch();
	}

	public void connect(String siteAddress, int sitePort, String siteUser, String adminSshHomeDir, String adminSshPrivateKey) throws JSchException {
		var knownHostFile = adminSshHomeDir + File.separator + ".ssh" + File.separator + "known_hosts";
		log.info("Adding known host file from: {}", knownHostFile);
		jsch.setKnownHosts(knownHostFile);
		jschSession = jsch.getSession(siteUser, siteAddress, sitePort);
		// Disable this for the moment as Jsch does not handle ssh-ed25519
		jschSession.setConfig("StrictHostKeyChecking", "no");
		log.info("Adding identity from private key file: {}", adminSshPrivateKey);

		try {
			jsch.addIdentity(adminSshPrivateKey);
		} catch (JSchException e) {
			log.error("Failed to load private key file {}. Make sure this exists", adminSshPrivateKey);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load private key file");
		}

		jschSession.connect(SESSION_TIMEOUT);
		channel = jschSession.openChannel("sftp");
		channel.connect(CHANNEL_TIMEOUT);
		channelSftp = (ChannelSftp) channel;
	}

	public void transferFilesToSite(List<SiteFile> siteFiles, List<FileThumb> thumbList) throws SftpException {
		if (!siteDirectoryExists(siteWwwRoot)) {
			log.error("The site main directory {} does not exist, file transfer is aborted", siteWwwRoot);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Site configuration is not set up correctly");
		}

		for (var siteFile : siteFiles) {
			var absolutePathConvertedFile =
					siteFileDirectory + File.separator + siteFile.getFileClass().shortName + File.separator + siteFile.getFilePath() + File.separator + siteFile.getFileName();
			log.debug("Transferring file {} to {}", absolutePathConvertedFile, siteWwwRoot);

			var targetSubDir = siteFile.getFileClass().shortName + File.separator + siteFile.getFilePath();
			var targetDir    = siteWwwRoot + File.separator + targetSubDir;

			// We remove the leading / from the subdir so that it can be later split correctly
			if (targetSubDir.startsWith(File.separator)) {
				targetSubDir = targetSubDir.substring(1);
			}

			if (!siteDirectoryExists(targetDir)) {
				log.debug("Creating site directory: {}", targetDir);
				var tmpArray     = targetSubDir.split("/");
				var siteDirPaths = new LinkedList<>(Arrays.asList(tmpArray));
				// We add as the last item the .thumb directory which will then contain the thumb files
				siteDirPaths.add(thumbSubDir);
				createRecursivelySiteDirectory(siteWwwRoot, siteDirPaths);
			} else {
				log.debug("Site directory {} already exists", targetDir);
			}

			channelSftp.put(absolutePathConvertedFile, targetDir + File.separator + siteFile.getFileName());
		}

		for (FileThumb fileThumb : thumbList) {
			var absolutePathThumbFile =
					siteFileDirectory + File.separator + fileThumb.getFilepath() + File.separator + fileThumb.getFilename();
			var targetDir = siteWwwRoot + File.separator + fileThumb.getSiteFile()
																	.getFilePath() + File.separator + thumbSubDir;
			log.info("Transferring thumb {} to {}", absolutePathThumbFile, targetDir);
			channelSftp.put(absolutePathThumbFile, targetDir);
		}
	}

	private void createRecursivelySiteDirectory(String mainDir, List<String> subDirList) throws SftpException {
		log.info("Called with main directory {} and list of directory elements {}", mainDir, subDirList);

		if (subDirList.isEmpty()) {
			log.info("No more directory elements to create");
			return;
		}

		String testDir = mainDir + File.separator + subDirList.getFirst();

		if (!siteDirectoryExists(testDir)) {
			log.info("Element dir {} did not exist as {}, creating it", subDirList.getFirst(), testDir);
			channelSftp.mkdir(testDir);
		}

		log.info("Popping first element from subdir list");
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

	public void close() {
		if (jschSession.isConnected()) {
			jschSession.disconnect();
		}
	}
}
