package net.termat.components.calendar;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

public class CalendarDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private CalendarPanel calendar;
	private Date date;
	
	public CalendarDialog(Dialog arg0) {
		super(arg0,true);
		init();
	}

	public CalendarDialog(Frame arg0) {
		super(arg0,true);
		init();
	}

	public void setDate(Date d){
		date=d;
		calendar.setDate(d);
	}
	
	public Date getDate(){
		return date;
	}
	
	private void init(){
		super.getContentPane().setLayout(new BorderLayout());
		super.setTitle("DateTimePicker");
		super.setResizable(false);
		super.setAlwaysOnTop(true);
		calendar=new CalendarPanel();
		super.getContentPane().add(calendar);
		super.setSize(250,204);
		JToolBar bar=new JToolBar();
		JButton bt0=new JButton(" O  K ");
		bt0.setBorder(BorderFactory.createEtchedBorder());
		bt0.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				date=calendar.getDate();
				setVisible(false);
			}
		});
		
		JButton bt1=new JButton("Cancel");
		bt1.setBorder(BorderFactory.createEtchedBorder());
		bt1.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				date=null;
				setVisible(false);
			}
		});
		
		bar.add(Box.createGlue());
		bar.add(bt0);
		bar.add(bt1);
		bar.setFloatable(false);
		super.getContentPane().add(bar,BorderLayout.SOUTH);
	}
	
	public static Date getDate(Component c){
		CalendarDialog cd=new CalendarDialog(JOptionPane.getFrameForComponent(c));
		cd.setLocationRelativeTo(c);
		cd.setVisible(true);
		return cd.date;
	}
	
	public static Date getDate(Component c,Date d){
		CalendarDialog cd=new CalendarDialog(JOptionPane.getFrameForComponent(c));
		cd.setLocationRelativeTo(c);
		cd.calendar.setDate(d);
		cd.setVisible(true);
		return cd.date;
	}
	
	public static Date getDate(Component c,int x,int y){
		CalendarDialog cd=new CalendarDialog(JOptionPane.getFrameForComponent(c));
		cd.setLocation(x, y);
		cd.setVisible(true);
		return cd.date;
	}
	
	public static Date getDate(Component c,int x,int y,Date d){
		CalendarDialog cd=new CalendarDialog(JOptionPane.getFrameForComponent(c));
		cd.setLocation(x, y);
		cd.calendar.setDate(d);
		cd.setVisible(true);
		return cd.date;
	}
	
	public static void main(String[] args){
		Date d=CalendarDialog.getDate(new JFrame());
		System.out.println(d);
	}
}
