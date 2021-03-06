/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;

import javax.xml.bind.Marshaller;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.part.FileEditorInput;

import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.XmlHelpers;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class EclipseEditorHelpers {

	public static void open_editor_sync(ByteArrayOutputStream output_stream, String full_path, String title)
			throws Exception {

		IEditorInput ei = new MyStorageEditorInput(output_stream, full_path, title);

		open_editor_sync(ei, full_path);
	}

	public static void open_editor_sync(Shell shell, IFile file, boolean create_missing)
			throws InternalException, PartInitException {

		if (file == null || file.exists() == false) {

			String title = file.getFullPath().toPortableString();

			if (create_missing) {

				if (EclipseMessageHelpers.show_confirmation(
						"'" + title + "' does not exist. " + "Do you want to create a new one?") == false) {

					return;
				}

				create_new_file_sync(shell, file, (InputStream) null, title, (IProgressMonitor) null);

			} else {

				throw new InternalException("'" + title + "' not found. Try to refresh (F5).");
			}
		}

		open_editor_sync(new FileEditorInput(file), file.getName());
	}

	private static void create_new_file_sync(Shell shell, IFile new_file_handle, InputStream initial_contents,
			String title, IProgressMonitor monitor) {

		CreateFileOperation op = new CreateFileOperation(new_file_handle, null, initial_contents, title);

		try {
			// see bug
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=219901
			// directly execute the operation so that the undo state is
			// not preserved. Making this undoable resulted in too many
			// accidental file deletions.
			//
			op.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(shell));

		} catch (final ExecutionException e) {

			shell.getDisplay().syncExec(new Runnable() {
				public void run() {

					EclipseMessageHelpers.show_error(e);
				}
			});
		}
	}

	public static IEditorPart open_editor_sync(IEditorInput editor_input, String file_name)
			throws PartInitException, InternalException {

		IEditorRegistry r = PlatformUI.getWorkbench().getEditorRegistry();

		IEditorDescriptor desc = r.getDefaultEditor(file_name);

		// Eclipse for RCP and RAP Developers' does not have SQL editor

		if (desc == null) {

			desc = r.getDefaultEditor("*.txt");
		}

		if (desc == null) {

			throw new InternalException("Cannot obtain editor descriptor.");
		}

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		IEditorPart editor_part;

		try {

			editor_part = page.openEditor(editor_input, desc.getId());

		} catch (Throwable ex) {

			desc = r.getDefaultEditor("*.txt");

			if (desc == null) {

				throw new InternalException("Cannot obtain editor descriptor.");
			}

			editor_part = page.openEditor(editor_input, desc.getId());
		}

		return editor_part;
	}

	public static void open_dto_xml_in_editor_sync(com.sqldalmaker.jaxb.dto.ObjectFactory object_factory,
			DtoClasses root) throws Exception {

		Marshaller marshaller = XmlHelpers.create_marshaller(object_factory.getClass().getPackage().getName(),
				Const.DTO_XSD);

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {

			marshaller.marshal(root, out);

			out.flush();

			open_editor_sync(out, "dto.xml", "_dto.xml"); // '%' throws URI exception in NB

		} finally {

			out.close();
		}
	}

	public static void open_dao_xml_in_editor_sync(String instance_name, String file_name, Object root)
			throws Exception {

		Marshaller marshaller = XmlHelpers.create_marshaller(instance_name, Const.DAO_XSD);

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {

			marshaller.marshal(root, out);

			out.flush();

			open_editor_sync(out, file_name, file_name);

		} finally {

			out.close();
		}
	}

	public static void open_tmp_field_tags_sync(String class_name, String ref, IProject project, final IEditor2 editor2)
			throws Exception {

		String project_root = EclipseHelpers.get_absolute_dir_path_str(project);

		Connection con = EclipseHelpers.get_connection(editor2);

		try {

			com.sqldalmaker.jaxb.dto.ObjectFactory object_factory = new com.sqldalmaker.jaxb.dto.ObjectFactory();

			DtoClasses root = object_factory.createDtoClasses();

			DtoClass cls = object_factory.createDtoClass();
			cls.setName(class_name);
			cls.setRef(ref);
			root.getDtoClass().add(cls);

			EclipseHelpers.gen_tmp_field_tags(con, object_factory, cls, project_root, editor2);

			open_dto_xml_in_editor_sync(object_factory, root);

		} finally {

			con.close();
		}
	}

	public static class MyStorageEditorInput extends PlatformObject implements IStorageEditorInput {

		// ^^ "extends PlatformObject" is copy-paste from FileEditorInput

		private IStorage storage;

		private String title;

		private String full_path;

		private ByteArrayOutputStream output_stream;

		private class MyStorage extends PlatformObject implements IStorage {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public Object getAdapter(Class adapter) {
				return super.getAdapter(adapter);
			}

			@Override
			public InputStream getContents() throws CoreException {

				try {

					// Return new stream as many times as they want. It prevents
					// java.io.IOException: Read error in
					// org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider.createFakeCompiltationUnit(CompilationUnitDocumentProvider.java:1090)
					// For example,
					// org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage.getContents()
					// creates it each time;

					ByteArrayInputStream res = new ByteArrayInputStream(output_stream.toByteArray());

					return res;

				} catch (Throwable e) {

					return null;
				}
			}

			@Override
			public IPath getFullPath() {
				return new Path(full_path);
			}

			// Returns the name of this storage.
			@Override
			public String getName() {
				return this.getClass().getName();
			}

			@Override
			public boolean isReadOnly() {
				return false;
			}

		} /////////////////////////////////// end of class MyStorageEditorInput.MyStorage

		public MyStorageEditorInput(ByteArrayOutputStream output_stream, String full_path, String title) {

			this.output_stream = output_stream;

			this.full_path = full_path;

			this.title = title;

			this.storage = new MyStorage();
		}

		@Override
		public IStorage getStorage() {
			return storage;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return ImageDescriptor.getMissingImageDescriptor();
		}

		@Override
		public String getName() {
			return title; // title of editor tab
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return title;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Object getAdapter(Class adapter) {
			return super.getAdapter(adapter);
		}

		@Override
		public boolean equals(Object obj) {

			return false; // always create a new one

			// equals(Object obj) is based on implementation from
			// FileEditorInput class.
			// It prevents opening multiple copies of the same resource
			// if (this == obj) {
			// return true;
			// }
			// if (!(obj instanceof PluginResourceEditorInput)) {
			// return false;
			// }
			// ByteArrayOutputStreamEditorInput other =
			// (ByteArrayOutputStreamEditorInput) obj;
			// return resPath.equals(other.resPath);
		}

		@Override
		public int hashCode() {
			return super.hashCode(); // just to avoid FireBug warning
		}

	} //////////////// end of class EclipseEditorUtils.MyStorageEditorInput

} ///////////////////////////////////// end of class EclipseEditorUtils
