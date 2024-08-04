package net.termat.tmgeo.web;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

import org.locationtech.jts.io.ParseException;

import net.termat.components.table.MyTableCellEditor;
import net.termat.components.table.MyTableCellRenderer;
import net.termat.tmgeo.data.VectorReader;
import net.termat.tmgeo.util.PCUtil;

public class WebTileDownloader {
	private JFrame frame;
	private JFileChooser chooser;
	private DefaultTableModel model;
	private File geojson;
	private File out;
	
	public WebTileDownloader() {
		frame=new JFrame();
		frame.setTitle("ElevEditorApp");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			SwingUtilities.updateComponentTreeUI(frame);
		}catch(Exception e){
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
				SwingUtilities.updateComponentTreeUI(frame);
			}catch(Exception ee){
				ee.printStackTrace();
			}
		}
		WindowAdapter wa=new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		};
		frame.addWindowListener(wa);
		frame.setSize(400, 360);
		frame.setResizable(false);
		frame.getContentPane().setLayout(new BorderLayout());
		JTabbedPane tab=new JTabbedPane();
		frame.getContentPane().add(tab,BorderLayout.CENTER);
		JPanel panel=new JPanel(new BorderLayout());
		tab.addTab("DATA", panel);
		JButton start=new JButton("Start");
		frame.getContentPane().add(start,BorderLayout.SOUTH);
		start.addActionListener(e->{
			int ch=JOptionPane.showConfirmDialog(frame, "処理を開始しますか？", "Info", JOptionPane.YES_NO_OPTION);
			if(ch==JOptionPane.YES_OPTION) {
				startProcess();
			}
		});
		final JTable table=new JTable();
		panel.add(table,BorderLayout.CENTER);
		table.setDefaultEditor(Object.class, new MyTableCellEditor());
		table.setDefaultRenderer(Object.class, new MyTableCellRenderer());
		model=new DefaultTableModel() {
			@Override
			public boolean isCellEditable(int row, int column) {
				if(getValueAt(row,column)instanceof JButton) {
					return true;
				}else {
					return column==1;
				}
			}
		};
		table.setModel(model);
		model.setColumnCount(2);
		model.setRowCount(8);
		model.setColumnIdentifiers(new String[] {"name","value"});
		table.setRowHeight(28);
		table.getColumnModel().getColumn(0).setMaxWidth(100);
		table.getColumnModel().getColumn(0).setMinWidth(100);
		model.setValueAt("座標系",0,0);
		model.setValueAt(getCrs(),0,1);
		model.setValueAt("標高PNG？",1,0);
		model.setValueAt(Boolean.FALSE,1,1);
		JComboBox<Integer> zoom=new JComboBox<Integer>();
		zoom.addItem(13);
		zoom.addItem(14);
		zoom.addItem(15);
		zoom.addItem(16);
		zoom.addItem(17);
		model.setValueAt("Zoomレベル",2,0);
		model.setValueAt(zoom,2,1);
		chooser=new JFileChooser();
		final JButton geo=new JButton("対象範囲");
		geo.addActionListener( e ->{
			chooser.setFileFilter(new FileFilter(){
				@Override
				public boolean accept(File f) {
					if(f.isDirectory()) {
						return true;
					}else {
						return f.getName().toLowerCase().endsWith(".geojson");
					}
				}

				@Override
				public String getDescription() {
					return "*.geojson";
				}
				
			});
			int op=chooser.showOpenDialog(frame);
			if(op==JFileChooser.APPROVE_OPTION) {
				File ff=chooser.getSelectedFile();
				geojson=ff;
				model.setValueAt(ff.getName(),3,1);
			}
		});
		
		model.setValueAt(geo,3,0);
		JComboBox<String> png=new JComboBox<String>();
		png.addItem("PNG");
		png.addItem("JPG");
		png.setSelectedIndex(0);
		model.setValueAt("画像形式",4,0);
		model.setValueAt(png,4,1);
		model.setValueAt("URL",5,0);
		model.setValueAt("",5,1);
		final JButton img=new JButton("出力先");
		img.addActionListener( e ->{
			chooser.setFileFilter(new FileFilter(){
				@Override
				public boolean accept(File f) {
					if(f.isDirectory()) {
						return true;
					}else {
						return f.getName().toLowerCase().endsWith(".png")||f.getName().toLowerCase().endsWith(".jpg");
					}
				}

				@Override
				public String getDescription() {
					return "*.png | *.jpg";
				}
				
			});
			int op=chooser.showOpenDialog(frame);
			if(op==JFileChooser.APPROVE_OPTION) {
				File ff=chooser.getSelectedFile();
				if(ff.getName().toLowerCase().endsWith(".png")) {
					model.setValueAt(ff.getName(),6,1);
					out=ff;
				}else if(ff.getName().toLowerCase().endsWith(".jpg")) {
					model.setValueAt(ff.getName(),6,1);
					out=ff;
				}else {
					@SuppressWarnings("unchecked")
					JComboBox<String> pp=(JComboBox<String> )model.getValueAt(4, 1);
					if(pp.getSelectedIndex()==0) {
						File o=new File(ff.getAbsolutePath()+".png");
						model.setValueAt(o.getName(),6,1);
						out=o;
					}else {
						File o=new File(ff.getAbsolutePath()+".jpg");
						model.setValueAt(o.getName(),6,1);
						out=o;
					}
				}
			}
		});
		model.setValueAt(img, 6, 0);
		JComboBox<Double> res=new JComboBox<Double>();
		res.addItem(0.5);
		res.addItem(1.0);
		res.addItem(2.0);
		res.addItem(2.5);
		res.addItem(5.0);
		res.addItem(10.0);
		res.addItem(20.0);
		res.setSelectedIndex(4);
		model.setValueAt("解像度", 7, 0);
		model.setValueAt(res, 7, 1);
	}
	
	private void close(){
		int id=JOptionPane.showConfirmDialog(frame, "Exit?", "Info", JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE);
		if(id==JOptionPane.YES_OPTION){
			frame.setVisible(false);
			System.exit(0);
		}
	}
	
	private JComboBox<String> getCrs(){
		JComboBox<String> crs=new JComboBox<String>();
		crs.addItem("平面直角第01系");
		crs.addItem("平面直角第02系");
		crs.addItem("平面直角第03系");
		crs.addItem("平面直角第04系");
		crs.addItem("平面直角第05系");
		crs.addItem("平面直角第06系");
		crs.addItem("平面直角第07系");
		crs.addItem("平面直角第08系");
		crs.addItem("平面直角第09系");
		crs.addItem("平面直角第10系");
		crs.addItem("平面直角第11系");
		crs.addItem("平面直角第12系");
		crs.addItem("平面直角第13系");
		crs.addItem("平面直角第14系");
		crs.addItem("平面直角第15系");
		crs.addItem("平面直角第16系");
		crs.addItem("平面直角第17系");
		crs.addItem("平面直角第18系");
		crs.addItem("平面直角第19系");
		crs.setSelectedIndex(5);
		crs.setMaximumSize(new Dimension(130,36));
		return crs;
	}
	
	private void startProcess() {
		int crs=((JComboBox)model.getValueAt(0, 1)).getSelectedIndex()+1;
		boolean dem=(Boolean)model.getValueAt(1, 1);
		int zoom=(Integer)((JComboBox)model.getValueAt(2, 1)).getSelectedItem();
		String exp=((JComboBox)model.getValueAt(4, 1)).getSelectedItem().toString();
		String url=(String)model.getValueAt(5, 1);
		double res=(Double)((JComboBox)model.getValueAt(7, 1)).getSelectedItem();
		final WebTile web=new WebTile(url,zoom,res);
		if(dem)web.setBackBround(new Color(PCUtil.NA));
		int epsg=6668+crs;
		try {
			final VectorReader vr=VectorReader.createReader(epsg, geojson);
			Runnable r=new Runnable() {
				@Override
				public void run() {
					try {
						Rectangle2D rect=vr.getBounds();
						web.create(crs, rect);
						ImageIO.write(web.getImage(), exp, out);
						AffineTransform af=new AffineTransform(
							res,0,0,-res,rect.getX(),rect.getY()+rect.getHeight());
						File tf=new File(out.getAbsolutePath().replace(".png", ".pgw").replace(".jpg", ".jgw"));
						PCUtil.writeTransform(af, tf);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			new Thread(r).start();
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		WebTileDownloader app=new WebTileDownloader();
		app.frame.setLocationRelativeTo(null);
		app.frame.setVisible(true);
	}
	
}
