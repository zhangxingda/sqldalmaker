/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.sqldalmaker.common.Const;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class ProjectFileHyperlinkDetector extends AbstractHyperlinkDetector {

	/**
	 * Tries to detect hyperlinks for the given region in the given text viewer and
	 * returns them.
	 * <p>
	 * In most of the cases only one hyperlink should be returned.
	 * </p>
	 *
	 * @param textViewer                the text viewer on which the hover popup
	 *                                  should be shown
	 * @param region                    the text range in the text viewer which is
	 *                                  used to detect the hyperlinks
	 * @param canShowMultipleHyperlinks tells whether the caller is able to show
	 *                                  multiple links to the user. If
	 *                                  <code>true</code> {@link IHyperlink#open()}
	 *                                  should directly open the link and not show
	 *                                  any additional UI to select from a list. If
	 *                                  <code>false</code> this method should only
	 *                                  return one hyperlink which upon
	 *                                  {@link IHyperlink#open()} may allow to
	 *                                  select from a list.
	 * @return the hyperlinks or <code>null</code> if no hyperlink was detected
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {

		// ===: as java-doc says, 'null' should be returned.
		// (in other case, default links do not work like
		// xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		// xsi:noNamespaceSchemaLocation="dao.xsd")

		IHyperlink[] NONE = null;// new IHyperlink[0]; !!!!!!!!!!!!

		try {

			if (region == null || textViewer == null) {

				return NONE;
			}

			IFile this_xml_file = XmlAttributeHelpers.get_current_file();

			if (this_xml_file == null) {

				return NONE;
			}

			boolean dto_xml = FileSearchHelpers.is_dto_xml(this_xml_file.getName());

			boolean dao_xml = FileSearchHelpers.is_dao_xml(this_xml_file.getName());

			// 'ref' attribute value is the name of resource file and cannot
			// contain '\"'

			int offset = region.getOffset();

			IDocument doc = textViewer.getDocument();

			if (doc == null) {

				return NONE;
			}

			String text = doc.get();

			IRegion hyperlink_region = XmlAttributeHelpers.get_attribute_value_region(offset, text);

			if (hyperlink_region == null) {

				return NONE;
			}

			String value;

			try {

				value = text.substring(hyperlink_region.getOffset(),
						hyperlink_region.getOffset() + hyperlink_region.getLength());

			} catch (Throwable e) {

				return NONE;
			}

			IContainer this_folder = this_xml_file.getParent();

			IResource profile = EclipseTargetLanguageHelpers.find_root_file(this_folder);

			String xml_metaprogram_folder_full_path = profile.getParent().getLocation().toPortableString();

			Settings settings = EclipseHelpers.load_settings(xml_metaprogram_folder_full_path);

			IFile file = null;

			int attr_offset = hyperlink_region.getOffset() - 2;

			boolean crateMissingFile = false;

			if (XmlAttributeHelpers.is_value_of("ref", attr_offset, text)) {

				if (dto_xml || dao_xml) {

					if (value.toLowerCase().endsWith(".sql") == false) {

						return NONE;
					}

					String path = settings.getFolders().getSql() + "/" + value;

					file = this_xml_file.getProject().getFile(path);

					if (file == null) {

						crateMissingFile = true;

						return NONE;
					}

				}

			} else if (dto_xml) {

				if (XmlAttributeHelpers.is_value_of("name", attr_offset, text)) {

					file = EclipseTargetLanguageHelpers.find_source_file_in_project_tree(this_xml_file.getProject(),
							settings, value, settings.getDto().getScope(), profile.getName());
				}

			} else if (dao_xml) {

				try {

					if (XmlAttributeHelpers.is_value_of("dto", attr_offset, text)) {

						IResource res = this_folder.findMember(Const.DTO_XML);

						if (res instanceof IFile) {

							file = (IFile) res;
						}
					}

				} catch (Throwable e) {

					e.printStackTrace();
				}
			}

			if (file == null) {

				return NONE;
			}

			ProjectFileHyperlink link = new ProjectFileHyperlink(hyperlink_region, file, crateMissingFile);

			return new IHyperlink[] { link };

		} catch (Throwable e) {

			e.printStackTrace();

			return NONE;
		}
	}
}