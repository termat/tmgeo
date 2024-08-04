package net.termat.tmgeo.sattelite.opensearch;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.locationtech.jts.io.ParseException;

import com.ibm.icu.text.DateFormat;

import net.termat.components.JProgressDialog;
import net.termat.tmgeo.data.VectorReader;

public class SearchPanel extends JPanel{
	private DefaultTableModel model;
	private JTable table;
	private static DateFormat df=DateFormat.getDateInstance(DateFormat.SHORT);
	private JProgressDialog pro;
	private List<CatalogLister> listeners;
	
	public SearchPanel() {
		super(new BorderLayout());
		table=new JTable();
		model=createSearchModel();
		table.setModel(model);
		table.setDefaultEditor(Object.class,new SSTableCellEditor());
		table.setDefaultRenderer(Object.class, new SSTableCellRenderer());
		table.setRowHeight(24);
		this.add(new JScrollPane(table));
		pro=new JProgressDialog(JOptionPane.getFrameForComponent(this));
		listeners=new ArrayList<>();
	}

	public void addCatalogListener(CatalogLister listener) {
		listeners.add(listener);
	}
	
	private DefaultTableModel createSearchModel() {
		DefaultTableModel m=new DefaultTableModel() {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column!=0;
			}
		};
		m.setColumnIdentifiers(new String[] {"Name","Value"});
		m.setColumnCount(2);
		m.setRowCount(6);	
		m.setValueAt("Start date", 0, 0);
		m.setValueAt(new Date(), 0, 1);
		m.setValueAt("End date", 1, 0);
		m.setValueAt(new Date(), 1, 1);
		JComboBox<String> com=new JComboBox<>();
		com.addItem("Sentinel1 / GRD");	//S1
		com.addItem("Sentinel1 / RAW");	//S1
		com.addItem("Sentinel1 / SLC");	//S1
		com.addItem("Sentinel2 / S2MSI1C");	//S2
		com.addItem("Sentinel2 / S2MSI2A");	//S2
//		com.addItem("Landsat5 / L1G");	//L5
//		com.addItem("Landsat5 / L1T");	//L5
//		com.addItem("Landsat7 / L1G");	//L7
//		com.addItem("Landsat7 / L1GT");	//L7
//		com.addItem("Landsat7 / L1T");	//L7	
//		com.addItem("Landsat8 / L1GT");	//L8
//		com.addItem("Landsat8 / L1T");	//L8
//		com.addItem("Landsat8 / L1TP");	//L8
//		com.addItem("Landsat8 / L2SP");	//L8
		com.setSelectedIndex(1);
		m.setValueAt("Satellite / ProductType", 2, 0);
		m.setValueAt(com, 2, 1);
		JSpinner sp1=new JSpinner();
		SpinnerNumberModel sm1=new SpinnerNumberModel(10,0,100,5);
		sp1.setModel(sm1);
		m.setValueAt("Clound Rate", 3, 0);
		m.setValueAt(sp1, 3, 1);
		JSpinner sp2=new JSpinner();
		SpinnerNumberModel sm2=new SpinnerNumberModel(10,10,100,10);
		sp2.setModel(sm2);
		m.setValueAt("Max Records", 4, 0);
		m.setValueAt(sp2, 4, 1);
		m.setValueAt("Target (geojson)", 5, 0);
		m.setValueAt(new File("(無指定)"), 5, 1);
		return m;
	}
	
	public void exex() throws ParseException, IOException {
		String satp=(String)((JComboBox<?>)model.getValueAt(2,1)).getSelectedItem();
		String[] sat=satp.split(" / ");
		Date st=(Date)model.getValueAt(0, 1);
		Date ed=(Date)model.getValueAt(1, 1);
		if(df.format(st).equals(df.format(ed))||st.getTime()>ed.getTime()) {
			throw new IOException("Start Time > End Time.");
		}
		File f=(File)model.getValueAt(5,1);
		if(!f.exists()) {
			throw new IOException("File not found.");
		}
		VectorReader vr=VectorReader.createReader(4326, f);
		String url=OpenSearch.createCatalogURL(sat[0], st, ed, vr.getBounds());
		int maxRec=(Integer)((JSpinner)model.getValueAt(4, 1)).getValue();
		url=OpenSearch.addMaxRecord(url, maxRec);
		int maxPerchent=(Integer)((JSpinner)model.getValueAt(3, 1)).getValue();
		if(!sat[0].equals("Sentinel1"))url=OpenSearch.addCloudCover(url,maxPerchent);
		url=OpenSearch.addProductType(url, sat[1]);
		pro.setLocationRelativeTo(null);
		pro.setVisible(true);
		OpenSearch.Listener sl=new OpenSearch.Listener() {
			@Override
			public void procCatalog(String str) {
				Runnable r=new Runnable() {
					public void run() {
						pro.setVisible(false);
						for(CatalogLister cl : listeners) {
							cl.provCatalog(str);
						}
					}
				};
				SwingUtilities.invokeLater(r);
			}

			@Override
			public void error(Exception e) {
				Runnable r=new Runnable() {
					public void run() {
						pro.setVisible(false);
					}
				};
				SwingUtilities.invokeLater(r);
				JOptionPane.showMessageDialog(table, e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
			}
		};
		OpenSearch.getCatalog(url, sl);
	}
	
	public interface CatalogLister{
		public void provCatalog(String json);
	}
}
