package net.termat.tmgeo.fomat.mbtiles;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.imintel.mbtiles4j.MBTilesWriteException;
import org.imintel.mbtiles4j.MBTilesWriter;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import net.termat.tmgeo.db.DBUtil;
import net.termat.tmgeo.util.MeshUtil;

public class TileDB {
	protected ConnectionSource connectionSource = null;
	protected Dao<Tile,Long> dao;
	
	public static TileDB open(File f) throws SQLException {
		TileDB ret=new TileDB();
		ret.connectDB(f.getAbsolutePath(), true);
		return ret;
	}
	
	public void connectDB(String dbName,boolean create) throws SQLException{
		try{
			if(!dbName.endsWith(".tdb"))dbName=dbName+".tdb";
			Class.forName("org.sqlite.JDBC");
			connectionSource = new JdbcConnectionSource("jdbc:sqlite:"+dbName);
			dao= DaoManager.createDao(connectionSource, Tile.class);
			if(create)TableUtils.createTableIfNotExists(connectionSource, Tile.class);
		}catch(SQLException e){
			e.printStackTrace();
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void writeMVT(byte[] b,int z,int x,int y) throws SQLException {
		Tile t=new Tile();
		t.bytes=b;
		t.z=z;
		t.x=x;
		t.y=y;
		dao.create(t);
	}
	
	public void writeMBTiles(MBTilesWriter mb) throws SQLException, MBTilesWriteException {
		List<Tile> tl=dao.queryForAll();
		for(Tile t : tl) {
			mb.addTile(t.bytes, t.z, t.x, t.y);
		}
	}
	
	public Tile getTile(int z,int x,int y) throws SQLException {
		QueryBuilder<Tile, Long> query=dao.queryBuilder();
		query.where().eq("z",z).and().eq("x", x).and().eq("y", y);
		List<Tile> list=dao.query(query.prepare());
		if(list.size()==0) {
			return null;
		}else {
			return list.get(0);
		}
	}
	
	public void createRasterTile(BufferedImage bi,AffineTransform af,int minZoom,int maxZoom,String ext) throws IOException, SQLException {
		int w=bi.getWidth();
		int h=bi.getHeight();
		Rectangle2D rect=new Rectangle2D.Double(0, 0, w,h);
		rect=af.createTransformedShape(rect).getBounds2D();
		AffineTransform at=null;
		try {
			at=af.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		for(int i=maxZoom;i>=minZoom;i--) {
			if(i==maxZoom) {
				List<Tile> list=new ArrayList<>();
				List<Point> li=MeshUtil.getTileList(rect, i);
				for(Point p : li) {
					Rectangle2D r=MeshUtil.getTileBounds(i, p.x, p.y);
					BufferedImage img=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
					double[] dd=new double[] {
							r.getWidth()/img.getWidth(),
							0,
							0,
							-r.getHeight()/img.getHeight(),
							r.getX(),
							r.getY()+r.getHeight()
						};
					AffineTransform aa=new AffineTransform(dd);
					for(int x=0;x<256;x++) {
						for(int y=0;y<256;y++) {
							Point2D pt=aa.transform(new Point2D.Double(x, y), new Point2D.Double());
							pt=at.transform(pt, new Point2D.Double());
							int xx=(int)Math.floor(pt.getX());
							int yy=(int)Math.floor(pt.getY());
							if(xx>=0&&xx<w&&yy>=0&&yy<h) {
								img.setRGB(x, y, bi.getRGB(xx,yy));
							}
						}
					}
					Tile t=new Tile();
					t.z=i;
					t.x=p.x;
					t.y=p.y;
					t.bytes=DBUtil.bi2Bytes(img, ext);
					list.add(t);
					if(list.size()>1000) {
						dao.create(list);
						list.clear();
					}
				}
				dao.create(list);
			}else {
				List<Tile> list=new ArrayList<>();
				List<Point> li=MeshUtil.getTileList(rect, i);
				for(Point p : li) {
					Rectangle2D r=MeshUtil.getTileBounds(i, p.x, p.y);
					BufferedImage img=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
					double[] dd=new double[] {
							r.getWidth()/img.getWidth(),
							0,
							0,
							-r.getHeight()/img.getHeight(),
							r.getX(),
							r.getY()+r.getHeight()
						};
					AffineTransform aa=new AffineTransform(dd);
					writeTile(img,aa,getTile(i+1,p.x*2,p.y*2),MeshUtil.getTileBounds(i+1, p.x*2, p.y*2));
					writeTile(img,aa,getTile(i+1,p.x*2+1,p.y*2),MeshUtil.getTileBounds(i+1, p.x*2+1, p.y*2));
					writeTile(img,aa,getTile(i+1,p.x*2,p.y*2+1),MeshUtil.getTileBounds(i+1, p.x*2, p.y*2+1));
					writeTile(img,aa,getTile(i+1,p.x*2+1,p.y*2+1),MeshUtil.getTileBounds(i+1, p.x*2+1, p.y*2+1));
					Tile t=new Tile();
					t.z=i;
					t.x=p.x;
					t.y=p.y;
					t.bytes=DBUtil.bi2Bytes(img, ext);
					list.add(t);
					if(list.size()>1000) {
						dao.create(list);
						list.clear();
					}
				}
				dao.create(list);
			}
		}
	}
	
	private static void writeTile(BufferedImage tile,AffineTransform af,Tile t,Rectangle2D r) throws IOException {
		if(t==null)return;
		BufferedImage tb=DBUtil.bytes2Bi(t.bytes);
		double[] dd=new double[] {
				r.getWidth()/tb.getWidth(),
				0,
				0,
				-r.getHeight()/tb.getHeight(),
				r.getX(),
				r.getY()+r.getHeight()
			};
		AffineTransform aa=new AffineTransform(dd);
		writeTileImage(tile,af,tb,aa);
	}
	
	public static void writeTileImage(BufferedImage tile,AffineTransform af1,BufferedImage src,AffineTransform af2) {
		Rectangle2D r1=new Rectangle2D.Double(0, 0, tile.getWidth(), tile.getHeight());
		r1=af2.createTransformedShape(r1).getBounds2D();
		Rectangle2D r2=new Rectangle2D.Double(0, 0, src.getWidth(), src.getHeight());
		r2=af2.createTransformedShape(r2).getBounds2D();
		if(r1.intersects(r2)) {
			AffineTransform at=null;
			try {
				at=af2.createInverse();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			int sw=src.getWidth();
			int sh=src.getHeight();
			int w=tile.getWidth();
			int h=tile.getHeight();
			for(int i=0;i<w;i++) {
				for(int j=0;j<h;j++) {
					Point2D p=af1.transform(new Point2D.Double(i, j), new Point2D.Double());
					p=at.transform(p, new Point2D.Double());
					int xx=(int)Math.floor(p.getX());
					int yy=(int)Math.floor(p.getY());
					if(xx>=0&&xx<sw&&yy>=0&&yy<sh) {
						tile.setRGB(i, j, src.getRGB(xx, yy));
					}
				}
			}
		}
	}
	

	static class Tile{
		@DatabaseField(generatedId=true)
		long id;
		@DatabaseField
		int z;
		@DatabaseField
		int x;
		@DatabaseField
		int y;
	    @DatabaseField(dataType = DataType.BYTE_ARRAY)
		byte[] bytes;
	}
	
}
