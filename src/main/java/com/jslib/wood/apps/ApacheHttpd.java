package com.jslib.wood.apps;

import java.io.File;
import java.io.IOException;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.AppContext;

final class ApacheHttpd {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(ApacheHttpd.class);

	private File DOC_ROOT;

	public ApacheHttpd(AppContext context) {
		log.trace("ApacheHttpdImpl(AppContext)");
		String docRootProperty = context.getProperty("server.doc.root");
		if (docRootProperty == null) {
			log.info("Missing <server.doc.root> environment property. Server configured without doc root. It can be added to conf/properties.xml file, e.g. <property name='server.doc.root' value='/var/www/vhosts/' />");
			return;
		}
		DOC_ROOT = new File(docRootProperty);
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
