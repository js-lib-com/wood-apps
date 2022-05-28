package com.jslib.wood.apps;

import java.io.File;
import java.io.IOException;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.contextparam.ContextParam;

final class ApacheHttpd {
	private static final Log log = LogFactory.getLog(ApacheHttpd.class);

	@ContextParam(name = "server.doc.root")
	private static File DOC_ROOT;

	public ApacheHttpd() {
		log.trace("ApacheHttpdImpl(AppContext)");
	}

	public File getProjectRoot(String path) throws IOException {
		File projectRoot = new File(DOC_ROOT, path);
		if (!projectRoot.exists()) {
			if (!projectRoot.mkdir()) {
				throw new IOException("Cannot create directory " + projectRoot);
			}
			log.info("Create project root directory |%s|.", projectRoot);
			// PosixFileAttributes parentAttributes = Files.readAttributes(DOC_ROOT.toPath(), PosixFileAttributes.class,
			// LinkOption.NOFOLLOW_LINKS);
			// PosixFileAttributeView attributes = Files.getFileAttributeView(projectRoot.toPath(),
			// PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
			// attributes.setOwner(parentAttributes.owner());
			// attributes.setGroup(parentAttributes.group());
		}
		return projectRoot;
	}
}
