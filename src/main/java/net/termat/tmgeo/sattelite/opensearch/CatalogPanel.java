package net.termat.tmgeo.sattelite.opensearch;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.google.gson.Gson;

import net.termat.tmgeo.sattelite.opensearch.SearchPanel.CatalogLister;

public class CatalogPanel extends JPanel implements CatalogLister{
	private JTable table;
	private DefaultTableModel model;
	private List<Map<String,Object>> list;
	private JPanel image;
	private BufferedImage bi=null;
	private double scale;
	private Map<Integer,BufferedImage> images=new HashMap<>();
	
	public CatalogPanel() {
		super(new BorderLayout());
		JSplitPane sp=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		super.add(sp,BorderLayout.CENTER);
		sp.setDividerLocation(400);
		sp.setDividerSize(20);
		sp.setOneTouchExpandable(true);
		model=new DefaultTableModel() {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
			
		};
		String[] title=new String[] {"sat","name","date","product"};
		model.setColumnIdentifiers(title);
		model.setColumnCount(title.length);
		model.setRowCount(0);
		table=new JTable();
		table.setModel(model);
		table.setRowHeight(24);
		sp.setLeftComponent(new JScrollPane(table));
		image=new JPanel() {
			@Override
			public void paint(Graphics g){
				if(bi!=null){
					g.clearRect(0, 0, getWidth(), getHeight());
					double w=(double)this.getWidth();
					double h=(double)this.getHeight();
					scale=Math.min(w/(double)bi.getWidth(),h/(double)bi.getHeight());
					((Graphics2D)g).scale(scale, scale);
					g.drawImage(bi, 0, 0, this);
				}else {
					g.clearRect(0, 0, getWidth(), getHeight());
				}
			}
		};
		sp.setRightComponent(image);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(e->{
			int row=table.getSelectedRow();
			if(row<0) {
				bi=null;
			}else {
				if(images.containsKey(row)) {
					bi=images.get(row);
					Runnable r=new Runnable() {
						public void run() {
							image.repaint();
							image.updateUI();
						}
					};
					SwingUtilities.invokeLater(r);
				}else {
					Runnable r=new Runnable() {
						public void run() {						
							Map<String,Object> map=list.get(row);
							@SuppressWarnings("unchecked")
							Map<String,Object> prop=(Map<String,Object>)map.get("properties");
							String url=(String)prop.get("thumbnail");
							if(url==null||url.equals("null")) {
								bi=null;
								image.repaint();
								image.updateUI();
							}else {
								try {
									bi=ImageIO.read(new URL(url));
									images.put(row, bi);
									image.repaint();
									image.updateUI();
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
						}
					};
					SwingUtilities.invokeLater(r);
				}
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void provCatalog(String json) {
		images.clear();
		Gson gson=new Gson();
		Map<String,Object> data=gson.fromJson(json, Map.class);
		list=(List<Map<String,Object>>)data.get("features");
		model.setRowCount(list.size());
		for(int i=0;i<list.size();i++) {
			Map<String,Object> map=list.get(i);
			System.out.println(map);
			Map<String,Object> prop=(Map<String,Object>)map.get("properties");
			model.setValueAt(prop.get("collection"), i, 0);
			model.setValueAt(prop.get("title"), i, 1);
			model.setValueAt(prop.get("startDate").toString().substring(0, 10), i, 2);	
			model.setValueAt(prop.get("productType"), i, 3);	
		}
	}

}
