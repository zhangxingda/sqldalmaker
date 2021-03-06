/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.eclipse;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wb.swt.ResourceManager;

import com.sqldalmaker.cg.Helpers;
import com.sqldalmaker.cg.IDaoCG;
import com.sqldalmaker.common.FileSearchHelpers;
import com.sqldalmaker.common.XmlParser;
import com.sqldalmaker.jaxb.dao.DaoClass;
import com.sqldalmaker.jaxb.settings.Settings;

/**
 *
 * @author sqldalmaker@gmail.com
 *
 */
public class UIEditorPageDAO extends Composite {

	private Filter filter;
	private IEditor2 editor2;

	public void setEditor2(IEditor2 editor2) {
		this.editor2 = editor2;
	}

	protected static final String STATUS_GENERATED = "Generated successfully";
	protected static final String STATUS_OK = "OK";

	// WindowBuilder fails with inheritance
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Text text;
	private TableViewer tableViewer;
	private Table table;
	private Action action_refresh;
	private Action action_selAll;
	private Action action_unselAll;
	private Action action_generate;
	private Action action_validate;
	private Action action_newXml;
	private Action action_openXml;
	private Action action_getCrudDao;
	private Action action_open_java;

	private ToolBarManager toolBarManager;
	private ToolBar toolBar1;
	private Composite composite_1;
	private Action action_FK;

	public ToolBar getToolBarManager() {
		return toolBar1;
	}

	/**
	 * Create the composite.
	 *
	 * @param parent
	 * @param style
	 */
	public UIEditorPageDAO(Composite parent, int style) {
		super(parent, style);
		createActions();
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});
		toolkit.adapt(this);
		toolkit.paintBordersFor(this);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.verticalSpacing = 8;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		setLayout(gridLayout);

		composite_1 = new Composite(this, SWT.NONE);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		toolkit.adapt(composite_1);
		toolkit.paintBordersFor(composite_1);
		composite_1.setLayout(new FillLayout(SWT.VERTICAL));

		toolBar1 = new ToolBar(composite_1, SWT.NONE);
		toolBarManager = new ToolBarManager(toolBar1);
		toolkit.adapt(toolBar1);
		toolkit.paintBordersFor(toolBar1);
		toolBarManager.add(action_newXml);
		toolBarManager.add(action_openXml);
		toolBarManager.add(action_open_java);
		toolBarManager.add(action_getCrudDao);
		toolBarManager.add(action_FK);
		toolBarManager.add(action_refresh);
		toolBarManager.add(action_unselAll);
		toolBarManager.add(action_selAll);
		toolBarManager.add(action_generate);
		toolBarManager.add(action_validate);

		text = toolkit.createText(composite_1, "", SWT.BORDER);
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				updateFilter();
			}
		});

		tableViewer = new TableViewer(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				doOnSelectionChanged();
			}
		});
		table = tableViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		// === panedrone: toolkit.adapt(table, false, false) leads to invalid selection
		// look in Linux
		// toolkit.adapt(table, false, false);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				Point pt = new Point(e.x, e.y);
				TableItem item = table.getItem(pt);
				if (item == null)
					return;
				int clicked_column_index = -1;
				for (int col = 0; col < 3; col++) {
					Rectangle rect = item.getBounds(col);
					if (rect.contains(pt)) {
						clicked_column_index = col;
						break;
					}
				}
				if (clicked_column_index == 0) {
					openXML();
				} else {
					openGeneratedSourceFile();
				}
			}
		});

		TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNewColumn = tableViewerColumn.getColumn();
		tblclmnNewColumn.setWidth(260);
		tblclmnNewColumn.setText("File");

		TableViewerColumn tableViewerColumn_1 = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tblclmnNewColumn_1 = tableViewerColumn_1.getColumn();
		tblclmnNewColumn_1.setWidth(300);
		tblclmnNewColumn_1.setText("State");
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		tableViewer.setContentProvider(new ArrayContentProvider());
		IBaseLabelProvider labelProvider = new ItemLabelProvider();
		filter = new Filter();
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.addFilter(filter);

		// http://www.java2s.com/Open-Source/Java-Document/IDE-Eclipse/ui/org/eclipse/ui/forms/examples/internal/rcp/SingleHeaderEditor.java.htm
		toolBarManager.update(true);
	}

	private void createActions() {
		{
			action_refresh = new Action("") {
				@Override
				public void run() {
					reloadTable(true);
				}
			};
			action_refresh.setToolTipText("Refesh");
			action_refresh
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/refresh.gif"));
		}
		{
			action_selAll = new Action("") {
				@Override
				public void run() {
					table.selectAll();
					doOnSelectionChanged();
				}
			};
			action_selAll.setToolTipText("Select all");
			action_selAll
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/text.gif"));
		}
		{
			action_getCrudDao = new Action("") {

				@Override
				public void run() {
					generate_crud_dao_xml();
				}
			};
			action_getCrudDao.setToolTipText("DAO CRUD assistant");
			action_getCrudDao
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/180.png"));
		}
		{
			action_unselAll = new Action("") {
				@Override
				public void run() {
					table.deselectAll();
					doOnSelectionChanged();
				}
			};
			action_unselAll.setToolTipText("Deselect all");
			action_unselAll
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/none.gif"));
		}
		{
			action_generate = new Action("") {
				@Override
				public void run() {
					generate();
				}
			};
			action_generate.setToolTipText("Generate selected");
			action_generate.setImageDescriptor(
					ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/compile-warning.png"));
		}
		{
			action_validate = new Action("") {
				@Override
				public void run() {

					try {

						validate();

					} catch (Throwable e) {

						e.printStackTrace();

						EclipseMessageHelpers.show_error(e);
					}
				}
			};
			action_validate.setToolTipText("Validate all");
			action_validate
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/validate.gif"));
		}
		{
			action_newXml = new Action("") {
				@Override
				public void run() {
					createXmlFile();
				}
			};
			action_newXml.setToolTipText("New XML file");
			action_newXml
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/new_xml.gif"));
		}
		{
			action_openXml = new Action("") {
				@Override
				public void run() {
					openXML();
				}
			};
			action_openXml.setToolTipText("Open XML file");
			action_openXml
					.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/xmldoc.gif"));
		}
		{
			action_open_java = new Action("") {
				@Override
				public void run() {
					openGeneratedSourceFile();
				}
			};
			action_open_java.setImageDescriptor(
					ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/GeneratedFile.gif"));
			action_open_java.setToolTipText("Go to generated source");
		}
		{
			action_FK = new Action("") {
				@Override
				public void run() {

					try {

						EclipseCrudXmlHelpers.get_fk_access_xml(getShell(), editor2);

					} catch (Throwable e) {

						e.printStackTrace();

						EclipseMessageHelpers.show_error(e);
					}
				}
			};
			action_FK.setImageDescriptor(ResourceManager.getImageDescriptor(UIEditorPageDAO.class, "/img/FK.gif"));
			action_FK.setToolTipText("FK access assistant");
		}
	}

	protected void openGeneratedSourceFile() {

		try {

			List<Item> items = prepareSelectedItems();

			if (items == null) {
				return;
			}

			IFile file = null;

			String rel = items.get(0).getRelativePath();
			String dao_class_name = Helpers.get_dao_class_name(rel);

			Settings settings = EclipseHelpers.load_settings(editor2);

			file = EclipseTargetLanguageHelpers.find_source_file_in_project_tree(editor2.get_project(), settings, dao_class_name,
					settings.getDao().getScope(), editor2.get_root_file_name());

			EclipseEditorHelpers.open_editor_sync(getShell(), file, false);

		} catch (Throwable e) {

			e.printStackTrace();

			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void openXML() {
		try {

			List<Item> items = prepareSelectedItems();

			if (items == null) {
				return;
			}

			String relative = items.get(0).getRelativePath();

			IFile file = editor2.find_metaprogram_file(relative);

			EclipseEditorHelpers.open_editor_sync(getShell(), file, true);

		} catch (Throwable e) {

			e.printStackTrace();

			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void generate_crud_dao_xml() {

		try {

			EclipseCrudXmlHelpers.get_crud_dao_xml(getShell(), editor2);

		} catch (Throwable e) {

			e.printStackTrace();

			EclipseMessageHelpers.show_error(e);
		}
	}

	protected void createXmlFile() {

		try {

			IFile file = UIDialogNewDaoXmlFile.open(getShell(), editor2);

			if (file != null) {

				try {

					reloadTable(true);

					EclipseEditorHelpers.open_editor_sync(getShell(), file, true);

				} catch (Throwable e1) {

					e1.printStackTrace();

					EclipseMessageHelpers.show_error(e1);
				}
			}

		} catch (Throwable e) {

			e.printStackTrace();

			EclipseMessageHelpers.show_error(e);
		}
	}

	private List<Item> prepareSelectedItems() {

		@SuppressWarnings("unchecked")
		List<Item> items = (List<Item>) tableViewer.getInput();

		if (items == null || items.size() == 0) {
			return null;
		}

		List<Item> res = new ArrayList<Item>();

		if (items.size() == 1) {
			Item item = items.get(0);
			item.setStatus("");
			res.add(item);
			return res;
		}

		int[] indexes = table.getSelectionIndices();

		if (indexes.length == 0) {

			// InternalHelpers.showError("Select DAO configurations");

			return null;
		}

		for (int row : indexes) {
			Item item = items.get(row);
			item.setStatus("");
			res.add(item);
		}

		return res;
	}

	private void generate() {

		final List<Item> items = prepareSelectedItems();

		if (items == null) {
			return;
		}

		// //////////////////////////////////////////

		tableViewer.refresh();

		// //////////////////////////////////////////

		EclipseSyncAction action = new EclipseSyncAction() {

			@Override
			public int get_total_work() {
				return items.size();
			}

			@Override
			public String get_name() {
				return "Code generation...";
			}

			@Override
			public void run_with_progress(IProgressMonitor monitor) throws Exception {

				boolean generated = false;

				monitor.subTask("Connecting...");

				Connection con = EclipseHelpers.get_connection(editor2);

				monitor.subTask("Connected.");

				try {

					EclipseConsoleHelpers.init_console();

					Settings settings = EclipseHelpers.load_settings(editor2);

					StringBuilder output_dir = new StringBuilder();
					// !!!! after 'try'
					IDaoCG gen = EclipseTargetLanguageHelpers.create_dao_cg(con, editor2.get_project(), editor2, settings, output_dir);

					String daoXsdFileName = editor2.get_dao_xsd_abs_path();

					String contextPath = DaoClass.class.getPackage().getName();
					XmlParser daoXml_Parser = new XmlParser(contextPath, daoXsdFileName);

					for (Item item : items) {

						if (monitor.isCanceled()) {
							return;
						}

						String daoXmlRelPath = item.getRelativePath();

						monitor.subTask(daoXmlRelPath);

						try {

							String dao_class_name = Helpers.get_dao_class_name(daoXmlRelPath);

							String daoXmlAbsPath = editor2.get_metaprogram_file_abs_path(daoXmlRelPath);

							DaoClass dao_class = daoXml_Parser.unmarshal(daoXmlAbsPath);

							String[] fileContent = gen.translate(dao_class_name, dao_class);

							String fileName = EclipseTargetLanguageHelpers.get_rel_path(editor2, output_dir, dao_class_name);

							EclipseHelpers.save_text_to_file(fileName, fileContent[0]);

							item.setStatus(STATUS_GENERATED);

							generated = true;

						} catch (Throwable ex) {

							String msg = ex.getMessage();

							if (msg == null) {
								msg = "???";
							}

							item.setStatus(msg);

							// throw ex; // outer 'catch' cannot read the
							// message

							// !!!! not Internal_Exception to show Exception
							// class

							// throw new Exception(ex);

							EclipseConsoleHelpers.add_error_msg(daoXmlRelPath, msg);
						}

						monitor.worked(1);
					}

				} finally {

					con.close();

					if (generated) {
						EclipseHelpers.refresh_project(editor2.get_project());
					}

					// Exception can occur at 3rd line (for example):
					// refresh first 3 lines

					// error lines are not generated but update them too

					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							tableViewer.refresh();
						}
					});
				}
			}
		};

		EclipseSyncActionHelper.run_with_progress(getShell(), action);
	}

	protected void validate() throws Exception {

		final List<Item> items = reloadTable();

		// ///////////////////////////////////////

		EclipseSyncAction action = new EclipseSyncAction() {

			@Override
			public int get_total_work() {
				return items.size();
			}

			@Override
			public String get_name() {
				return "Validation...";
			}

			@Override
			public void run_with_progress(IProgressMonitor monitor) throws Exception {

				monitor.subTask("Connecting...");

				Connection con = EclipseHelpers.get_connection(editor2);

				monitor.subTask("Connected.");

				try {

					EclipseConsoleHelpers.init_console();

					Settings settings = EclipseHelpers.load_settings(editor2);

					StringBuilder output_dir = new StringBuilder();
					// !!!! after 'try'
					IDaoCG gen = EclipseTargetLanguageHelpers.create_dao_cg(con, editor2.get_project(), editor2, settings, output_dir);

					String daoXsdFileName = editor2.get_dao_xsd_abs_path();

					String contextPath = DaoClass.class.getPackage().getName();
					XmlParser daoXml_Parser = new XmlParser(contextPath, daoXsdFileName);

					for (int i = 0; i < items.size(); i++) {

						if (monitor.isCanceled()) {
							return;
						}

						String daoXmlRelPath = items.get(i).getRelativePath();

						monitor.subTask(daoXmlRelPath);

						try {

							StringBuilder validationBuff = new StringBuilder();

							String dao_class_name = Helpers.get_dao_class_name(daoXmlRelPath);

							String daoXmlAbsPath = editor2.get_metaprogram_file_abs_path(daoXmlRelPath);

							DaoClass dao_class = daoXml_Parser.unmarshal(daoXmlAbsPath);

							String[] fileContent = gen.translate(dao_class_name, dao_class);

							String fileName = EclipseTargetLanguageHelpers.get_rel_path(editor2, output_dir, dao_class_name);

							String oldText = Helpers.load_text_from_file(fileName);

							if (oldText == null) {
								validationBuff.append("Generated file is missing");
							} else {
								String text = fileContent[0];
								if (!oldText.equals(text)) {
									validationBuff.append("Generated file is out of date");
								}
							}

							String status = validationBuff.toString();

							if (status.length() == 0) {

								items.get(i).setStatus(STATUS_OK);

							} else {

								items.get(i).setStatus(status);
								EclipseConsoleHelpers.add_error_msg(daoXmlRelPath, status);
							}

						} catch (Throwable ex) {

							ex.printStackTrace();

							String msg = ex.getMessage();

							items.get(i).setStatus(msg);

							EclipseConsoleHelpers.add_error_msg(daoXmlRelPath, msg);
						}

						monitor.worked(1);
					}

				} finally {

					con.close();

					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							tableViewer.refresh();
						}
					});
				}
			}
		};

		EclipseSyncActionHelper.run_with_progress(getShell(), action);
	}

	// private IJavaProject getJavaProject() {
	//
	// IJavaProject jproject = JavaCore.create(project);
	//
	// return jproject;
	// }

	private class ItemLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {

		public String getColumnText(Object element, int columnIndex) {
			String result = "";
			Item item = (Item) element;
			switch (columnIndex) {
			case 0:
				result = item.getRelativePath();
				break;
			case 1:
				result = item.getStatus();
				break;
			default:
				break;
			}
			return result;
		}

		@Override
		public Image getColumnImage(Object arg0, int arg1) {
			return null;
		}

		@Override
		public Color getBackground(Object arg0) {
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			Item item = (Item) element;

			if (item.getStatus() != null && item.getStatus().length() > 0 && STATUS_OK.equals(item.getStatus()) == false
					&& STATUS_GENERATED.equals(item.getStatus()) == false) {

				return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED);
			}
			return null;
		}
	}

	static class Item {

		private String relativePath;
		private String status;

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getRelativePath() {
			return relativePath;
		}

		public void setRelativePath(String relativePath) {
			this.relativePath = relativePath;
		}

	}

	private class Filter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {

			if (searchString == null || searchString.length() == 0) {
				return true;
			}

			Item item = (Item) element;
			if (item.getRelativePath().matches(searchString)) {
				return true;
			}

			return false;
		}

		private String searchString;

		public void setSearchText(String s) {
			// Search must be a substring of the existing value
			this.searchString = ".*" + s + ".*";
		}
	}

	protected void updateFilter() {
		filter.setSearchText(text.getText());
		tableViewer.refresh(); // fires selectionChanged
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	private ArrayList<Item> reloadTable() throws Exception {

		final ArrayList<Item> items = new ArrayList<Item>();

		try {

			FileSearchHelpers.IFile_List fileList = new FileSearchHelpers.IFile_List() {

				@Override
				public void add(String fileName) {

					Item item = new Item();

					item.setRelativePath(fileName);

					items.add(item);
				}
			};

			FileSearchHelpers.enum_dao_xml_file_names(editor2.get_metaprogram_folder_abs_path(), fileList);

		} finally {

			tableViewer.setInput(items);
			tableViewer.refresh();

			// tableViewer.refresh(); NOT REQUIRED
			doOnSelectionChanged();

			boolean enable = items.size() > 0;
			action_validate.setEnabled(enable);
		}

		return items;
	}

	public void reloadTable(boolean showErrorMsg) {

		try {

			reloadTable();

		} catch (Throwable e) {

			if (showErrorMsg) {

				e.printStackTrace();

				EclipseMessageHelpers.show_error(e);
			}
		}
	}

	private void doOnSelectionChanged() {

		@SuppressWarnings("unchecked")
		List<Item> items = (List<Item>) tableViewer.getInput();

		boolean enabled;

		if (items.size() == 1) {

			enabled = true;

		} else {

			int[] indexes = table.getSelectionIndices();
			enabled = indexes.length > 0;
		}

		action_generate.setEnabled(enabled);
		action_openXml.setEnabled(enabled);
		action_open_java.setEnabled(enabled);
	}
}
