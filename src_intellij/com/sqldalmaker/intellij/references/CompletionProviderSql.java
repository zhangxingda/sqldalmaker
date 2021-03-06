/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.references;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.intellij.ui.IdeaHelpers;
import com.sqldalmaker.intellij.ui.IdeaTargetLanguageHelpers;
import com.sqldalmaker.jaxb.settings.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sqldalmaker@gmail.com
 * on 17.02.2015.
 */
public class CompletionProviderSql extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {

        XmlAttributeValue attribute_value = PsiTreeUtil.getParentOfType(parameters.getPosition(), XmlAttributeValue.class, false);

        if (attribute_value == null) {

            return;
        }

        PsiElement attr_element = attribute_value.getParent();

        if (!(attr_element instanceof XmlAttribute)) {

            return;
        }

        PsiFile containing_file = attr_element.getContainingFile();

        if (containing_file == null) {

            return;
        }

        VirtualFile this_xml_file = IdeaReferenceCompletion.find_virtual_file(containing_file);

        if (this_xml_file == null) {

            return;
        }

        VirtualFile xml_file_dir = this_xml_file.getParent();

        if (xml_file_dir == null) {

            return;
        }

        Project project = attribute_value.getProject(); // @NotNull

        VirtualFile project_dir;

        try {

            project_dir = IdeaHelpers.get_project_base_dir(project);

        } catch (Exception e) {

            e.printStackTrace();

            return;
        }

        String sql_root_rel_path;

        final Settings settings;

        try {

            settings = IdeaHelpers.load_settings(xml_file_dir.getPath());

            sql_root_rel_path = settings.getFolders().getSql();

        } catch (Exception e) {

            e.printStackTrace();

            return;
        }

        VirtualFile sql_base_dir = project_dir.findFileByRelativePath(sql_root_rel_path);

        if (sql_base_dir == null) {

            return;
        }

        ArrayList<String> res = new ArrayList<String>();

        find_sql_files_recursive(sql_base_dir, res);

        String sql_base_dir_path = sql_base_dir.getPath();

        for (String s : res) {

            s = s.substring(sql_base_dir_path.length() + 1);

            result.addElement(LookupElementBuilder.create(s));
        }
    }

    private void find_sql_files_recursive(VirtualFile base_dir, final List<String> res) {

        VfsUtilCore.visitChildrenRecursively(base_dir, new VirtualFileVisitor() {

            @Override
            public boolean visitFile(@NotNull VirtualFile file) {

                if (!file.isDirectory()) {

                    String path = file.getPath();

                    if (Helpers.is_sql_file_ref(path)) {

                        res.add(path);
                    }
                }

                return true;
            }
        });
    }
}