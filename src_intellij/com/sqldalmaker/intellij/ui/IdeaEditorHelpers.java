/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.sqldalmaker.cg.DbUtils;
import com.sqldalmaker.cg.FieldInfo;
import com.sqldalmaker.cg.FieldNamesMode;
import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.common.XmlHelpers;
import com.sqldalmaker.jaxb.dto.DtoClass;
import com.sqldalmaker.jaxb.dto.DtoClasses;
import com.sqldalmaker.jaxb.dto.ObjectFactory;
import com.sqldalmaker.jaxb.settings.Settings;

import java.sql.Connection;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class IdeaEditorHelpers {

    private static void open_in_editor(Project project, VirtualFile file) {

        FileEditorManager fem = FileEditorManager.getInstance(project);

        fem.openFile(file, true, true);
    }

    public static void open_local_file_in_editor(Project project, VirtualFile root_file, String rel_path) throws Exception {

        VirtualFile file = root_file.getParent().findFileByRelativePath(rel_path);

        if (file == null) {

            throw new Exception("Not found: " + rel_path);
        }

        open_in_editor(project, file);
    }

    public static void open_local_file_in_editor(Project project, VirtualFile file) {

        open_in_editor(project, file);
    }

    public static void open_module_file_in_editor(Project project, String rel_path) throws Exception {

        VirtualFile project_dir = IdeaHelpers.get_project_base_dir(project);

        VirtualFile file = project_dir.findFileByRelativePath(rel_path);

        if (file == null) {

            throw new Exception("Not found: " + rel_path);
        }

        open_in_editor(project, file);
    }

    public static void open_text_in_new_editor(Project project, String file_name, String text) {

        FileEditorManager fem = FileEditorManager.getInstance(project);

        String virtual_file_name = "_" + file_name; // '%' throws URI exception in NB

        // http://grepcode.com/file/repository.grepcode.com/java/ext/com.jetbrains/intellij-idea/9.0.4/com/intellij/openapi/vfs/VirtualFile.java
        // If an in-memory implementation of VirtualFile is required, LightVirtualFile from the com.intellij.testFramework package (Extended API) can be used.

        try {

            open_text_in_new_editor(fem, virtual_file_name, text);

        } catch (Throwable e1) {

            e1.printStackTrace();

            IdeaMessageHelpers.show_error_in_ui_thread(e1);
        }
    }

    private static void open_text_in_new_editor(FileEditorManager fem, String virtual_file_name, String text) throws Exception {

        // http://grepcode.com/file/repository.grepcode.com/java/ext/com.jetbrains/intellij-idea/9.0.4/com/intellij/openapi/vfs/VirtualFile.java
        // If an in-memory implementation of VirtualFile is required, LightVirtualFile from the com.intellij.testFramework package (Extended API) can be used.

        VirtualFile file;
        FileEditor[] res;

        // Big problems with \r\n:
        // 1. copy-paste do not work
        // 2. VM files are not opened with exception like 'Wrong line separators...'
        //
        // Maybe, the hint is in here: 'Line breaks in a document are always normalized to \n.'
        // (http://confluence.jetbrains.com/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview)
        //
        // '\r' for mac os?
        // Mac OS up to version 9 and OS-9 (http://en.wikipedia.org/wiki/Line_breaking_character)

        text = text.replace("\r\n", "\n");

        try {

            file = new LightVirtualFile(virtual_file_name, text);
            res = fem.openFile(file, true, true); // for VM: it may throw something like 'Wrong line separators...'

        } catch (Throwable e) {

            // for VM: it will show error log message anyway, but il will open it and will not throw, at least
            file = new LightVirtualFile(virtual_file_name, FileTypes.PLAIN_TEXT, text);

            res = fem.openFile(file, true, true);
        }

        if (/*res == null ||*/ res.length == 0) {

            file = new LightVirtualFile(virtual_file_name, FileTypes.PLAIN_TEXT, text);

            res = fem.openFile(file, true, true);

            if (/*res == null ||*/ res.length == 0) {

                throw new Exception("Cannot open the file in editor: " + virtual_file_name);
            }
        }
    }

    public static void open_or_activate_jar_resource_in_editor(Project project, String res_name, String title) {

        open_or_activate_jar_file_in_editor(project, "resources", res_name, title);
    }

    public static void open_or_activate_jar_file_in_editor(Project project, String res_path, String res_name, String title) {

        FileEditorManager fem = FileEditorManager.getInstance(project);

        String virtual_file_name = "%" + title; // to make unique

        VirtualFile file = null;

        VirtualFile[] open_files = fem.getOpenFiles();

        for (VirtualFile open_file : open_files) {

            if (virtual_file_name.equals(open_file.getName())) {

                file = open_file;

                break;
            }
        }

        try {

            if (file != null) {

                fem.openFile(file, true, true);

            } else {

                String text_from_jar = IdeaHelpers.read_from_jar_file(res_path, res_name);

                open_text_in_new_editor(fem, virtual_file_name, text_from_jar);
            }

        } catch (Throwable e1) {

            e1.printStackTrace();

            IdeaMessageHelpers.show_error_in_ui_thread(e1);
        }
    }

    public static void open_dto_xml(Project project, VirtualFile root_file) {

        try {

            VirtualFile dto_xml = root_file.getParent().findFileByRelativePath(Const.DTO_XML);

            if (dto_xml == null) {

                throw new InternalException("File '" + Const.DTO_XML + "' not found");
            }

            open_in_editor(project, dto_xml);

        } catch (Exception e) {

            IdeaMessageHelpers.show_error_in_ui_thread(e);

            e.printStackTrace();
        }
    }

    public static void open_settings_xml(Project project, VirtualFile root_file) {

        try {

            VirtualFile dto_xml = root_file.getParent().findFileByRelativePath(Const.SETTINGS_XML);

            if (dto_xml == null) {

                throw new InternalException("File '" + Const.SETTINGS_XML + "' not found");
            }

            open_in_editor(project, dto_xml);

        } catch (Exception e) {

            IdeaMessageHelpers.show_error_in_ui_thread(e);

            e.printStackTrace();
        }
    }

    private static void gen_tmp_field_tags(Connection connection,
                                           ObjectFactory object_factory,
                                           DtoClass element, String sql_root_folder_full_path) throws Exception {

        DbUtils md = new DbUtils(connection, FieldNamesMode.AS_IS, null);

        ArrayList<FieldInfo> fields = md.get_dto_field_info(sql_root_folder_full_path, element);

        for (FieldInfo f : fields) {

            DtoClass.Field df = object_factory.createDtoClassField();
            df.setColumn(f.getColumnName());
            df.setJavaType(f.getType());
            element.getField().add(df);
        }
    }

    public static void gen_tmp_field_tags(String class_name, String ref,
                                          Project project, VirtualFile root_file) throws Exception {

        Settings settings = IdeaHelpers.load_settings(root_file);

        Connection con = IdeaHelpers.get_connection(project, settings);

        try {

            ObjectFactory object_factory = new ObjectFactory();

            DtoClasses dto_classes = object_factory.createDtoClasses();

            DtoClass cls = object_factory.createDtoClass();
            cls.setName(class_name);
            cls.setRef(ref);
            dto_classes.getDtoClass().add(cls);

            VirtualFile project_dir = IdeaHelpers.get_project_base_dir(project);

            final String module_root = project_dir.getPath();

            String sql_root_folder_full_path = module_root + "/" + settings.getFolders().getSql();

            gen_tmp_field_tags(con, object_factory, cls, sql_root_folder_full_path);

            open_dto_xml_in_editor(object_factory, project, dto_classes, true);

        } finally {

            con.close();
        }
    }

    public static void open_dto_xml_in_editor(ObjectFactory object_factory, Project project,
                                              DtoClasses dto_classes, boolean remove_java_lang) throws Exception {

        String text = XmlHelpers.get_dto_xml_text(object_factory, dto_classes, remove_java_lang);

        open_text_in_new_editor(project, "dto.xml", text);
    }
}
