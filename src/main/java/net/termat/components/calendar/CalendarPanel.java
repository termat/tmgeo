package net.termat.components.calendar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class CalendarPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Date now;
	private JTable table;
	private DateTableModel model;
	private String[] week={"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
	private Calendar cal;
	private JSpinner ysp,msp,hsp;
	
	public CalendarPanel(){
		super(new BorderLayout());
		table=new JTable();
		super.add(table);
		table.setDefaultRenderer(Object.class, new DateRenderer());
		table.setColumnSelectionAllowed(false);
		table.setRowSelectionAllowed(false);
		model=new DateTableModel();
		table.setModel(model);
		cal=Calendar.getInstance();
		now=cal.getTime();
		model.setCalendar(now);
		table.addMouseListener(new TMouseListener());
		
		JPanel p=new JPanel();
		BoxLayout b=new BoxLayout(p,BoxLayout.X_AXIS);
		p.setLayout(b);
		ysp=new JSpinner();
		SpinnerDateModel sd0=new SpinnerDateModel();
		sd0.setValue(cal.getTime());
		ysp.setModel(sd0);
		JSpinner.DateEditor je0=new JSpinner.DateEditor(ysp,"yyyy");
		ysp.setEditor(je0);
		ysp.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent arg0) {
				updateCalendar();
			}
		});
		msp=new JSpinner();
		SpinnerDateModel sd1=new SpinnerDateModel();
		msp.setModel(sd1);
		JSpinner.DateEditor je1=new JSpinner.DateEditor(msp,"MM");
		msp.setEditor(je1);
		msp.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent arg0) {
				updateCalendar();
			}
		});
		p.add(ysp);
		p.add(new JLabel("年"));
		p.add(msp);
		p.add(new JLabel("月"));
		
		hsp=new JSpinner();
		SpinnerDateModel sd2=new SpinnerDateModel();
		sd2.setValue(cal.getTime());
		hsp.setModel(sd2);
		JSpinner.DateEditor je2=new JSpinner.DateEditor(hsp,"HH:mm:ss");
		hsp.setEditor(je2);
		hsp.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent arg0) {
				updateCalendar();
			}
		});
		super.add(p,BorderLayout.NORTH);
		JPanel p2=new JPanel();
		BoxLayout b2=new BoxLayout(p2,BoxLayout.X_AXIS);
		p2.setLayout(b2);
		p2.add(Box.createGlue());
		p2.add(new JLabel("Time"));
		p2.add(hsp);
		super.add(p2,BorderLayout.SOUTH);
		super.setPreferredSize(new Dimension(175,187));
	}
	
	private void updateCalendar(){
		cal.setTime((Date)ysp.getValue());
		int YY=cal.get(Calendar.YEAR);
		cal.setTime((Date)msp.getValue());
		int MM=cal.get(Calendar.MONTH);
		cal.setTime((Date)hsp.getValue());
		int hh=cal.get(Calendar.HOUR_OF_DAY);
		int mm=cal.get(Calendar.MINUTE);
		int ss=cal.get(Calendar.SECOND);
		cal.setTime(now);
		cal.set(Calendar.YEAR, YY);
		cal.set(Calendar.MONTH, MM);
		cal.set(Calendar.HOUR_OF_DAY, hh);
		cal.set(Calendar.MINUTE, mm);
		cal.set(Calendar.SECOND, ss);
		now=cal.getTime();
		model.setCalendar(now);
	}
	
	public Date getDate(){
		System.out.println(now);
		return now;
	}
	
	public void setDate(Date d){
		now=d;
		ysp.setValue(d);
		msp.setValue(d);
		hsp.setValue(d);
		updateCalendar();
	}
	
	private class DateTableModel extends DefaultTableModel{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		DateTableModel(){
			super();
			super.setColumnCount(7);
			super.setRowCount(7);
			for(int i=0;i<7;i++){
				super.setValueAt(week[i], 0, i);
			}
		}
		
		private void clear(){
			for(int i=1;i<7;i++){
				for(int j=0;j<7;j++){
					super.setValueAt("", i, j);
				}
			}
		}
		
		private void setCalendar(Date d){
			clear();
			cal.setTime(d);
			int month=cal.get(Calendar.MONTH);
			int date=cal.get(Calendar.DATE);
			cal.set(Calendar.DATE, 1);
			int col=getWeekCol(cal.get(Calendar.DAY_OF_WEEK));
			cal.set(Calendar.MONTH,(month+1)%12);
			cal.set(Calendar.DATE, 0);
			int lg=cal.get(Calendar.DATE);
			int row=1;
			int sr=0,sc=0;
			for(int i=1;i<lg+1;i++){
				model.setValueAt(i, row, col%7);
				if(i==date){sr=row;sc=col%7;}
				col++;
				if(col%7==0)row++;
			}
			table.changeSelection(sr,sc,false,false);
		}
		
		private int getWeekCol(int day){
			switch(day){
				case Calendar.MONDAY:
					return 1;
				case Calendar.TUESDAY:
					return 2;
				case Calendar.WEDNESDAY:
					return 3;
				case Calendar.THURSDAY:
					return 4;
				case Calendar.FRIDAY:
					return 5;
				case Calendar.SATURDAY:
					return 6;
				default:
					return 0;
			}
		}
		
		@Override
		public boolean isCellEditable(int arg0, int arg1) {
			return false;
		}
		
	}
	
	public class DateRenderer extends DefaultTableCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final Color ec=new Color(240,240,255);
		  private final Color bb=new Color(245,245,245);
		  public DateRenderer() {
		    super();
		    setOpaque(true);
		    setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
		  }
		  public Component getTableCellRendererComponent(
		        JTable table, Object value,
		        boolean isSelected, boolean hasFocus,
		        int row, int column) {
		    super.getTableCellRendererComponent(table, value, 
		        isSelected, hasFocus, row, column);
		    if(row==0){
			      setBackground(bb);
			      setForeground(Color.GRAY);
		    }else{
		    	setForeground(table.getForeground());
		    	setBackground(table.getBackground());
		    }
		    int c=table.getSelectedColumn();
		    int r=table.getSelectedRow();
		    if(row==r&&column==c&&row!=0){
		    	setBackground(ec);
		    	setForeground(Color.BLUE);
		    }
		    setHorizontalAlignment((value instanceof Number)?RIGHT:CENTER);
		    return this;
		}
	}
	
	private class TMouseListener extends MouseAdapter{

		@Override
		public void mouseClicked(MouseEvent arg0) {
			JTable t=(JTable)arg0.getSource();
			int row=t.getSelectedRow();
			int col=t.getSelectedColumn();
			if(row!=-1&&col!=-1){
				Object o=model.getValueAt(row, col);
				if(o instanceof Integer){
					cal.setTime(now);
					cal.set(Calendar.DATE, (Integer)o);
					now=cal.getTime();
				}
			}
		}
	}
}
