package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.response.file.DirectoryNodeResponse;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileSystemService {
	@Value("${vempain.admin.file.site-file-directory}")
	private String siteFileDirectory;

	public List<DirectoryNodeResponse> getConvertedDirectoryTree() {
		File rootDir = new File(siteFileDirectory);
		if (!rootDir.exists() || !rootDir.isDirectory()) {
			throw new IllegalArgumentException("Invalid root directory path");
		}

		var rootList = new ArrayList<DirectoryNodeResponse>();

		// Get list of sub directories in root directory
		File[] files = rootDir.listFiles();

		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()
					&& !Objects.equals(file.toString(), siteFileDirectory + File.separator + FileTypeEnum.THUMB.shortName)) {
					rootList.add(buildDirectoryTree(file));
				}
			}
		}

		return rootList;
	}

	private DirectoryNodeResponse buildDirectoryTree(File dir) {
		var node = DirectoryNodeResponse.builder()
										.directoryName(dir.getName())
										.build();

		File[] files = dir.listFiles();

		if (files != null && files.length > 0) {
			List<DirectoryNodeResponse> children = new ArrayList<>();

			for (File file : files) {
				if (file.isDirectory()) {
					children.add(buildDirectoryTree(file));
				}
			}

			node.setChildren(children);
		}
		return node;
	}
}
