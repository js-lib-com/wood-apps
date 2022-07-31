package com.jslib.wood.apps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import js.io.FilesInputStream;
import js.io.FilesIterator;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.ITinyContainer;
import js.util.Files;

@Singleton
@PermitAll
public class AppsManager {
	private static final Log log = LogFactory.getLog(AppsManager.class);

	private final ITinyContainer container;
	private final ApacheHttpd apache;

	@Inject
	public AppsManager(ITinyContainer container, ApacheHttpd apache) {
		this.container = container;
		this.apache = apache;
	}

	@Path("dirty-files")
	public List<String> getDirtyFiles(String targetDir, SortedMap<String, byte[]> sourceFiles, boolean removeStaleFiles) throws IOException {
		String docRoot = apache.getProjectRoot(targetDir).getPath();
		List<String> dirtyFiles = new ArrayList<String>();

		SortedMap<String, byte[]> targetFiles = new TreeMap<String, byte[]>();
		for (String file : FilesIterator.getRelativeNamesIterator(docRoot)) {
			targetFiles.put(Files.path2unix(file), Files.getFileDigest(new File(docRoot, file)));
		}

		Iterator<Map.Entry<String, byte[]>> sourceFilesIterator = sourceFiles.entrySet().iterator();
		Iterator<Map.Entry<String, byte[]>> targetFilesIterator = targetFiles.entrySet().iterator();

		SOURCE_FILES_LOOP: while (sourceFilesIterator.hasNext()) {
			if (!targetFilesIterator.hasNext()) {
				break;
			}
			Map.Entry<String, byte[]> sourceFileEntry = sourceFilesIterator.next();
			Map.Entry<String, byte[]> targetFileEntry = targetFilesIterator.next();

			if (sourceFileEntry.getKey().equals(targetFileEntry.getKey())) {
				// here files are equals; check file digest to see if changed
				addDirtyFile(dirtyFiles, sourceFileEntry, targetFileEntry);
				continue;
			}

			// source and target iterators synchronization is lost; attempt to regain it

			// remove target file by file till found one existing into source files or end of target files
			while (!sourceFiles.containsKey(targetFileEntry.getKey())) {
				if (removeStaleFiles) {
					// target file is not present on source and need to be erased
					deleteTargetFile(docRoot, targetFileEntry);
				}
				if (!targetFilesIterator.hasNext()) {
					break SOURCE_FILES_LOOP;
				}
				targetFileEntry = targetFilesIterator.next();
			}

			// here we know the target file is present into sources
			// add sources file by file until reach the target
			for (;;) {
				if (sourceFileEntry.getKey().equals(targetFileEntry.getKey())) {
					// here source and target files are equals; check file digest to see if changed
					addDirtyFile(dirtyFiles, sourceFileEntry, targetFileEntry);
					continue SOURCE_FILES_LOOP;
				}
				addDirtyFile(dirtyFiles, sourceFileEntry);
				if (!sourceFilesIterator.hasNext()) {
					break SOURCE_FILES_LOOP;
				}
				sourceFileEntry = sourceFilesIterator.next();
			}
		}

		// if there are more unprocessed source files just add to dirty files list
		while (sourceFilesIterator.hasNext()) {
			addDirtyFile(dirtyFiles, sourceFilesIterator.next());
		}

		// if there are more unprocessed target files remove them
		if (removeStaleFiles) {
			while (targetFilesIterator.hasNext()) {
				deleteTargetFile(docRoot, targetFilesIterator.next());
			}
		}

		return dirtyFiles;
	}

	private void deleteTargetFile(String targetDir, Map.Entry<String, byte[]> targetFileEntry) {
		File targetFile = new File(targetDir, targetFileEntry.getKey());
		if (!targetFile.delete()) {
			badFilesArchive("Fail to delete target file |%s|.", targetFile.getAbsolutePath());
		} else {
			log.trace("Remove target file |%s|.", targetFile.getAbsoluteFile());
		}
	}

	private static void addDirtyFile(List<String> dirtyFiles, Map.Entry<String, byte[]> sourceFileEntry) {
		dirtyFiles.add(sourceFileEntry.getKey());
		// log.trace("Add dirty file |%s|.", sourceFileEntry.getKey());
		System.out.printf("Add dirty file |%s|.\r\n", sourceFileEntry.getKey());
	}

	private static void addDirtyFile(List<String> dirtyFiles, Map.Entry<String, byte[]> sourceFileEntry, Map.Entry<String, byte[]> targetFileEntry) {
		if (!Arrays.equals(sourceFileEntry.getValue(), targetFileEntry.getValue())) {
			addDirtyFile(dirtyFiles, sourceFileEntry);
		}
	}

	@Path("synchronize")
	public void synchronize(String targetDir, FilesInputStream files) throws IOException {
		File docRoot = apache.getProjectRoot(targetDir);
		if (!docRoot.exists()) {
			badFilesArchive("Target directory |%s| does not exist.", docRoot);
		}
		if (!docRoot.isDirectory()) {
			badFilesArchive("Target directory |%s| is a regular file.", docRoot);
		}
		log.debug("Synchronize files archive on target directory |%s|.", docRoot);

		for (File file : files) {
			File targetFile = new File(docRoot, file.getPath());
			log.trace("Synchronize file |%s|.", targetFile);
			if (targetFile.exists()) {
				if (!targetFile.delete()) {
					badFilesArchive("Fail to delete target file |%s|.", targetFile.getAbsolutePath());
				}
			}
			Files.mkdirs(targetFile);
			files.copy(targetFile);
		}
	}

	private void badFilesArchive(String message, Object... args) throws AppsException {
		RequestContext context = container.getInstance(RequestContext.class);
		throw new AppsException("Fail to process files archive uploaded from |%s|. %s", context.getRemoteHost(), String.format(message, args));
	}
}
