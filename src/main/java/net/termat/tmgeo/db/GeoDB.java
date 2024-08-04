package net.termat.tmgeo.db;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import net.termat.tmgeo.util.PCUtil;

public class GeoDB {
	protected ConnectionSource connectionSource = null;
	protected  Dao<Index,Long> indexDao;
	protected  Dao<DBData,Long> dataDao;
	protected  List<ChangeListener> listeners=new ArrayList<>();

	public static GeoDB open(File f) throws SQLException {
		GeoDB ret=new GeoDB();
		ret.connectDB(f.getAbsolutePath(), true);
		return ret;
	}
	
	public void connectDB(String dbName,boolean create) throws SQLException{
		try{
			if(!dbName.endsWith(".db"))dbName=dbName+".db";
			Class.forName("org.sqlite.JDBC");
			connectionSource = new JdbcConnectionSource("jdbc:sqlite:"+dbName);
			indexDao= DaoManager.createDao(connectionSource, Index.class);
			if(create)TableUtils.createTableIfNotExists(connectionSource, Index.class);
			dataDao= DaoManager.createDao(connectionSource, DBData.class);
			if(create)TableUtils.createTableIfNotExists(connectionSource, DBData.class);
		}catch(SQLException e){
			e.printStackTrace();
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void close() throws Exception {
		try {
			connectionSource.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void dbchange(){
		ChangeEvent evt=new ChangeEvent(this);
		for(ChangeListener c:listeners)c.stateChanged(evt);
	}
	
	public void add(Index index,DBData data) throws SQLException {
		index.key=System.currentTimeMillis();
		data.key=index.key;
		dataDao.create(data);
		indexDao.create(index);
		dbchange();
	}
	
	public void update(Index index) throws SQLException {
		indexDao.update(index);
	}
	
	public void update(Index index,DBData data) throws SQLException {
		QueryBuilder<DBData, Long> query=dataDao.queryBuilder();
		query.where().eq("key",index.key);
		List<DBData> list=dataDao.query(query.prepare());
		DBData src=list.get(0);
		index.key=src.key;
		dataDao.update(src);
	}
	
	public void delete(Index index) throws SQLException{
		indexDao.delete(index);
		QueryBuilder<DBData, Long> query=dataDao.queryBuilder();
		query.where().eq("key",index.key);
		List<DBData> list=dataDao.query(query.prepare());
		DBData img=list.get(0);
		dataDao.delete(img);
		dbchange();
	}
	
	public String[] getTypeName() throws SQLException {
		List<Index> li=indexDao.queryForAll();
		Set<String> ret=new HashSet<>();
		for(Index i : li)ret.add(i.type);
		String[] str=ret.toArray(new String[ret.size()]);
		Arrays.sort(str);	
		return str;
	}
	
	public List<Index> getIndexes(String type) throws SQLException{
		QueryBuilder<Index, Long> query=indexDao.queryBuilder();
		query.where().eq("type",type);
		return indexDao.query(query.prepare());
	}
	
	public List<Index> getIndexes(String name,String type) throws SQLException{
		QueryBuilder<Index, Long> query=indexDao.queryBuilder();
		query.where().eq("type",type).and().eq("name", name);
		return indexDao.query(query.prepare());
	}
	
	public List<Index> getIndexes(String type,Rectangle2D rect) throws SQLException{
		List<Index> li=getIndexes(type);
		List<Index> ret=new ArrayList<>();
		for(Index i : li) {
			if(rect.intersects(i.getBounds()))ret.add(i);
		}
		return null;
	}
	
	public List<Index> getIndexesByName(String name) throws SQLException{
		QueryBuilder<Index, Long> query=indexDao.queryBuilder();
		query.where().eq("name",name);
		return indexDao.query(query.prepare());
	}
	
	public DBData getData(Index index) throws SQLException, IOException{
		QueryBuilder<DBData, Long> query=dataDao.queryBuilder();
		query.where().eq("key",index.key);
		List<DBData> list=dataDao.query(query.prepare());
		return list.get(0);
	}
	
	public Object getExpantionImage(Index index,int exp) throws SQLException, IOException{
		DBData data=this.getData(index);
		if(data.type==DBData.Type.IMAGE) {
			BufferedImage img=new BufferedImage(index.width+exp*2,index.height+exp*2,BufferedImage.TYPE_INT_RGB);
			Graphics2D g=img.createGraphics();
			g.setBackground(new Color(PCUtil.NA));
			g.clearRect(0, 0, img.getWidth(), img.getHeight());
			AffineTransform af=index.getTransform();
			BufferedImage tmp=(BufferedImage)data.get(index);
			g.drawImage(tmp, exp, exp, null);
			g.dispose();
			double[] param=new double[]{af.getScaleX(),0,0,af.getScaleY(),af.getTranslateX()-af.getScaleX()*exp,af.getTranslateY()-af.getScaleY()*exp};
			af=new AffineTransform(param);
			AffineTransform iaf=null;
			try {
				iaf=af.createInverse();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			Rectangle2D rect=af.createTransformedShape(new Rectangle2D.Double(0,0,img.getWidth(),img.getHeight())).getBounds2D();
			QueryBuilder<Index, Long> query=indexDao.queryBuilder();
			query.where().eq("type",index.type);
			List<Index> list=indexDao.query(query.prepare());
			for(Index i : list){
				if(i.equals(index))continue;
				if(i.getBounds().intersects(rect)){
					DBData dd=this.getData(i);
					tmp=(BufferedImage)dd.get(i);
					AffineTransform at=i.getTransform();
					for(int x=0;x<tmp.getWidth();x++){
						for(int y=0;y<tmp.getHeight();y++){
							Point2D p=at.transform(new Point2D.Double(x,y), new Point2D.Double());
							if(rect.contains(p)){
								Point2D pt=iaf.transform(p, new Point2D.Double());
								int xx=(int)Math.round(pt.getX());
								int yy=(int)Math.round(pt.getY());
								if(xx>=0&&xx<img.getWidth()&&yy>=0&&yy<img.getHeight()){
									img.setRGB(xx, yy, tmp.getRGB(x, y));
								}
							}
						}
					}
				}
			}
			return img;
		}else if(data.type==DBData.Type.ARRAY) {
			float[][] img=new float[index.width+exp*2][index.height+exp*2];
			AffineTransform af=index.getTransform();
			float[][] tmp=(float[][])data.get(index);
			double[] param=new double[]{af.getScaleX(),0,0,af.getScaleY(),af.getTranslateX()-af.getScaleX()*exp,af.getTranslateY()-af.getScaleY()*exp};
			af=new AffineTransform(param);
			AffineTransform iaf=null;
			try {
				iaf=af.createInverse();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			Rectangle2D rect=af.createTransformedShape(new Rectangle2D.Double(0,0,img.length,img[0].length)).getBounds2D();
			QueryBuilder<Index, Long> query=indexDao.queryBuilder();
			query.where().eq("type",index.type);
			List<Index> list=indexDao.query(query.prepare());
			for(Index i : list){
				if(i.equals(index))continue;
				if(i.getBounds().intersects(rect)){
					DBData dd=this.getData(i);
					tmp=(float[][])dd.get(i);
					AffineTransform at=i.getTransform();
					for(int x=0;x<tmp.length;x++){
						for(int y=0;y<tmp[0].length;y++){
							Point2D p=at.transform(new Point2D.Double(x,y), new Point2D.Double());
							if(rect.contains(p)){
								Point2D pt=iaf.transform(p, new Point2D.Double());
								int xx=(int)Math.round(pt.getX());
								int yy=(int)Math.round(pt.getY());
								if(xx>=0&&xx<img.length&&yy>=0&&yy<img[0].length){
									img[xx][yy]=tmp[x][y];
								}
							}
						}
					}
				}
			}
			return img;
			
		}else {
			return null;
		}
	}
	
}
