package net.termat.tmgeo.fomat.mbtiles;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import net.termat.tmgeo.db.DBUtil;

public class MBHelperDB {
	protected ConnectionSource connectionSource = null;
	protected Dao<Image,Long> dao;
	
	public static MBHelperDB open(File f) throws SQLException {
		MBHelperDB ret=new MBHelperDB();
		ret.connectDB(f.getAbsolutePath(), true);
		return ret;
	}
	
	public Map<BufferedImage,AffineTransform> getImage(Rectangle2D r) throws SQLException, IOException{
		QueryBuilder<Image, Long> query=dao.queryBuilder();	
		query.where().ge("minx",r.getX()).or().lt("maxx", r.getX()+r.getWidth()).and().ge("miny", r.getY()).or().lt("maxy", r.getY()+r.getHeight());
		List<Image> list=dao.query(query.prepare());;
		
		System.out.println(list.size());
		
		Map<BufferedImage,AffineTransform> ret=new HashMap<>();
		for(Image im : list) {
			BufferedImage ii=DBUtil.bytes2Bi(im.bytes);
			double[] dd=new double[] {
				(im.maxx-im.minx)/ii.getWidth(),
				0,
				0,
				-(im.maxy-im.miny)/ii.getHeight(),
				im.minx,
				im.maxy
			};
			AffineTransform aa=new AffineTransform(dd);
			ret.put(ii, aa);
		}
		return ret;
	}
	
	
	public void addImage(BufferedImage bi,AffineTransform af,String ext) throws SQLException {
		int w=bi.getWidth();
		int h=bi.getHeight();
		List<Image> list=new ArrayList<>();
		for(int x=0;x<w;x=x+200) {
			for(int y=0;y<h;y=y+200) {
				Rectangle2D r=new Rectangle2D.Double(x, y, x+200, y+200);
				r=af.createTransformedShape(r).getBounds2D();
				int ww=Math.min(200,w-x);
				int hh=Math.min(200,w-y);
				BufferedImage tmp=new BufferedImage(ww,hh,BufferedImage.TYPE_INT_RGB);
				for(int i=0;i<ww;i++) {
					for(int j=0;j<hh;j++) {
						tmp.setRGB(i, j, bi.getRGB(x+i, y+j));
					}
				}
				Image im=new Image();
				im.minx=r.getX();
				im.miny=r.getY();
				im.maxx=r.getX()+r.getWidth();
				im.maxy=r.getY()+r.getHeight();
				try {
					im.bytes=DBUtil.bi2Bytes(tmp, ext);
				} catch (IOException e) {
					e.printStackTrace();
				}
				list.add(im);
			}
		}
		dao.create(list);
	}
	
	public void connectDB(String dbName,boolean create) throws SQLException{
		try{
			if(!dbName.endsWith(".hdb"))dbName=dbName+".hdb";
			Class.forName("org.sqlite.JDBC");
			connectionSource = new JdbcConnectionSource("jdbc:sqlite:"+dbName);
			dao= DaoManager.createDao(connectionSource, Image.class);
			if(create)TableUtils.createTableIfNotExists(connectionSource, Image.class);
		}catch(SQLException e){
			e.printStackTrace();
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	static class Image{
		@DatabaseField(generatedId=true)
		long id;
		@DatabaseField
		double minx;
		@DatabaseField
		double miny;
		@DatabaseField
		double maxx;
		@DatabaseField
		double maxy;
	    @DatabaseField(dataType = DataType.BYTE_ARRAY)
		byte[] bytes;
	}
}
