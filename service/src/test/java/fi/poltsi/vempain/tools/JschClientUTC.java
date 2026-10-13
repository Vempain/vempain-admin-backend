package fi.poltsi.vempain.tools;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.common.api.FileTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The SFTP transfer of a gallery: every site file is sent together with its thumbnails and reported right after, so that a publish
 * task can count the files while the transfer is still running.
 */
@ExtendWith(MockitoExtension.class)
class JschClientUTC {

	@Mock
	private ChannelSftp channelSftp;

	private JschClient jschClient;

	@BeforeEach
	void setUp() {
		jschClient = new JschClient();
		ReflectionTestUtils.setField(jschClient, "channelSftp", channelSftp);
		ReflectionTestUtils.setField(jschClient, "siteWwwRoot", "/www");
		ReflectionTestUtils.setField(jschClient, "siteFileDirectory", "/site-files");
		ReflectionTestUtils.setField(jschClient, "thumbSubDir", ".thumb");
	}

	@Test
	void transfersEachFileWithItsThumbnailsAndReportsItBeforeTheNextOne() throws Exception {
		var first = SiteFile.builder()
							.id(1L)
							.fileType(FileTypeEnum.IMAGE)
							.filePath("2024/trip")
							.fileName("a.jpg")
							.build();
		var second = SiteFile.builder()
							 .id(2L)
							 .fileType(FileTypeEnum.IMAGE)
							 .filePath("2024/trip")
							 .fileName("b.jpg")
							 .build();
		var firstThumb = FileThumb.builder()
								  .parentId(1L)
								  .parentType(FileTypeEnum.IMAGE)
								  .filepath("thumbs")
								  .filename("a.jpeg")
								  .build();
		firstThumb.setSiteFile(first);
		var orphanThumb = FileThumb.builder()
								   .parentId(9L)
								   .parentType(FileTypeEnum.IMAGE)
								   .filepath("thumbs")
								   .filename("orphan.jpeg")
								   .build();
		orphanThumb.setSiteFile(first);
		var reported = new ArrayList<String>();

		// lstat answers without throwing: every directory exists already
		jschClient.transferFilesToSite(List.of(first, second), List.of(orphanThumb, firstThumb), file -> reported.add(file.getFileName()));

		assertThat(reported).containsExactly("a.jpg", "b.jpg");
		var order = inOrder(channelSftp);
		order.verify(channelSftp)
			 .put("/site-files/image/2024/trip/a.jpg", "/www/image/2024/trip/a.jpg");
		order.verify(channelSftp)
			 .put("/site-files/thumbs/a.jpeg", "/www/image/2024/trip/.thumb");
		order.verify(channelSftp)
			 .put("/site-files/image/2024/trip/b.jpg", "/www/image/2024/trip/b.jpg");
		// A thumbnail without a listed site file is still transferred, after the files
		order.verify(channelSftp)
			 .put("/site-files/thumbs/orphan.jpeg", "/www/image/2024/trip/.thumb");
		order.verify(channelSftp, org.mockito.Mockito.never())
			 .put(anyString(), anyString());
	}

	@Test
	void transferWithoutACallbackStillSendsEverything() throws Exception {
		var file = SiteFile.builder()
						   .id(1L)
						   .fileType(FileTypeEnum.IMAGE)
						   .filePath("x")
						   .fileName("a.jpg")
						   .build();

		jschClient.transferFilesToSite(List.of(file), List.of());

		org.mockito.Mockito.verify(channelSftp)
						   .put("/site-files/image/x/a.jpg", "/www/image/x/a.jpg");
	}

	@Test
	void connectOpensASessionWithTheKnownHostsAndIdentityOfTheAdminUser() throws Exception {
		var jsch = mock(JSch.class);
		var session = mock(Session.class);
		var sftp = mock(ChannelSftp.class);
		ReflectionTestUtils.setField(jschClient, "jsch", jsch);
		when(jsch.getSession("vempain-site", "site.example", 2222)).thenReturn(session);
		when(session.openChannel("sftp")).thenReturn(sftp);

		jschClient.connect("site.example", 2222, "vempain-site", "/home/vempain-admin", "/home/vempain-admin/.ssh/id_ed25519");

		var order = inOrder(jsch, session, sftp);
		order.verify(jsch)
			 .setKnownHosts("/home/vempain-admin/.ssh/known_hosts");
		order.verify(session)
			 .setConfig("StrictHostKeyChecking", "no");
		order.verify(jsch)
			 .addIdentity("/home/vempain-admin/.ssh/id_ed25519");
		order.verify(session)
			 .connect(26_000);
		order.verify(sftp)
			 .connect(25_000);
		assertEquals(sftp, ReflectionTestUtils.getField(jschClient, "channelSftp"));

		when(session.isConnected()).thenReturn(true);
		jschClient.close();
		verify(session).disconnect();
	}

	@Test
	void anUnreadablePrivateKeyIsAnInternalError() throws Exception {
		var jsch = mock(JSch.class);
		var session = mock(Session.class);
		ReflectionTestUtils.setField(jschClient, "jsch", jsch);
		when(jsch.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
		doThrow(new JSchException("invalid privatekey")).when(jsch)
														.addIdentity("/missing/key");

		var thrown = assertThrows(ResponseStatusException.class,
								  () -> jschClient.connect("site.example", 22, "vempain-site", "/home/vempain-admin", "/missing/key"));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, thrown.getStatusCode());
		verify(session, never()).connect(anyInt());
	}

	@Test
	void closeLeavesADisconnectedSessionAlone() {
		var session = mock(Session.class);
		ReflectionTestUtils.setField(jschClient, "jschSession", session);
		when(session.isConnected()).thenReturn(false);

		jschClient.close();

		verify(session, never()).disconnect();
	}

	@Test
	void aMissingSiteRootAbortsTheTransfer() throws Exception {
		doThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "no such file")).when(channelSftp)
																				   .lstat("/www");

		var thrown = assertThrows(ResponseStatusException.class, () -> jschClient.transferFilesToSite(List.of(), List.of(), null));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, thrown.getStatusCode());
		verify(channelSftp, never()).put(anyString(), anyString());
	}

	@Test
	void missingTargetDirectoriesAreCreatedOneLevelAtATimeIncludingTheThumbDirectory() throws Exception {
		var file = SiteFile.builder()
						   .id(1L)
						   .fileType(FileTypeEnum.IMAGE)
						   .filePath("2024/trip")
						   .fileName("a.jpg")
						   .build();
		// The root and the first level exist, everything below is missing; a permission problem is reported as missing too
		org.mockito.Mockito.doAnswer(invocation -> {
			   String path = invocation.getArgument(0);
			   switch (path) {
				   case "/www/image/2024/trip", "/www/image/2024" -> throw new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "missing");
				   case "/www/image/2024/trip/.thumb" -> throw new SftpException(ChannelSftp.SSH_FX_PERMISSION_DENIED, "denied");
				   default -> {
					   return null;
				   }
			   }
		   })
						   .when(channelSftp)
						   .lstat(anyString());

		jschClient.transferFilesToSite(List.of(file), List.of(), null);

		var order = inOrder(channelSftp);
		order.verify(channelSftp)
			 .mkdir("/www/image/2024");
		order.verify(channelSftp)
			 .mkdir("/www/image/2024/trip");
		order.verify(channelSftp)
			 .mkdir("/www/image/2024/trip/.thumb");
		order.verify(channelSftp)
			 .put("/site-files/image/2024/trip/a.jpg", "/www/image/2024/trip/a.jpg");
		verify(channelSftp, never()).mkdir("/www/image");
	}
}
