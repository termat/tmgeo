package net.termat.tmgeo.sattelite.opensearch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.locationtech.jts.io.ParseException;

import net.termat.components.JCloseButtonTabbedPane;
import net.termat.components.solver.SolverTable;

public class TestApp {
	private JFrame frame;
	private SolverTable solver;
	private SearchPanel search;
	private CatalogPanel catalog;
	
	public TestApp() {
		frame=new JFrame();
		frame.setTitle("OpenSearch (ESA Copernicus)");
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
		frame.getContentPane().setLayout(new BorderLayout());
		frame.setSize(600,800);
		JMenuBar menu=new JMenuBar();
		JMenu m1=new JMenu("File");
		m1.setMnemonic('F');
		menu.add(m1);
		JMenuItem it1=new JMenuItem("Exit");
		m1.add(it1);
		it1.addActionListener(e->{
			close();
		});
		frame.setJMenuBar(menu);
		JCloseButtonTabbedPane tab=new JCloseButtonTabbedPane();
		frame.getContentPane().add(tab,BorderLayout.CENTER);
		search=new SearchPanel();
		tab.addTab("検索条件", search);
		catalog=new CatalogPanel();
		tab.addTab("検索結果", catalog);
		search.addCatalogListener(catalog);
		search.addCatalogListener(new SearchPanel.CatalogLister() {
			@Override
			public void provCatalog(String json) {
				tab.setSelectedIndex(1);
			}
		});
		solver=new SolverTable();
		tab.addTab("ダウンロード", solver);
		JToolBar tool=new JToolBar();
		tool.setFloatable(false);
		tool.add(Box.createGlue());
		tool.addSeparator();
		JButton exec=new JButton("実行");
		exec.setPreferredSize(new Dimension(36,36));
		exec.addActionListener(e->{
			if(tab.getSelectedIndex()==0) {
				try {
					search.exex();
				} catch (ParseException | IOException e1) {
					JOptionPane.showMessageDialog(frame, e1.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		tool.add(exec);
		tool.addSeparator();
		frame.getContentPane().add(tool,BorderLayout.SOUTH);
	}
	
	private void close(){
		int id=JOptionPane.showConfirmDialog(frame, "終了しますか？", "Info", JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE);
		if(id==JOptionPane.YES_OPTION){
			frame.setVisible(false);
			System.exit(0);
		}
	}
	
	public static void main(String[] args) {
		TestApp app=new TestApp();
		app.frame.setLocationRelativeTo(null);
		app.frame.setVisible(true);
	}
}
