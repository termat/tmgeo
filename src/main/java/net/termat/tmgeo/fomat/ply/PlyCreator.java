package net.termat.tmgeo.fomat.ply;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.locationtech.jts.io.ParseException;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;
import org.tinfour.standard.IncrementalTin;
import org.tinfour.utils.TriangleCollector;

import net.termat.tmgeo.data.VectorReader;
import net.termat.tmgeo.util.PCUtil;

public class PlyCreator{
	private List<Vertex> vertexs;
	private List<Color> colors;
	private IncrementalTin tin;
	private Shape sp=null;
	private int num_tri=0;
	
	public static void mainx(String[] args) throws IOException, ParseException {
		VectorReader vr=VectorReader.createReader(new File("H:\\仕事\\R06市原市\\市原GIS\\対象地域poly\\対象地域poly.shp"));
		/*
		File dir=new File("H:\\仕事\\R06市原市\\ply");
		for(File f : dir.listFiles()) {
			if(f.getName().toLowerCase().endsWith(".xyz")) {
				System.out.println(f.getName());
				PlyCreator pc=new PlyCreator(f,false,vr.getShapeList());
				pc.writePLY(new File(f.getAbsolutePath().replace(".xyz", ".ply")));
			}
		}
		*/
		File f=new File("H:\\仕事\\R06市原市\\ply\\傾斜区分図.xyz");
		PlyCreator pc=new PlyCreator(f,false,vr.getShapeList());
		pc.writePLY(new File(f.getAbsolutePath().replace(".xyz", ".ply")));
		
	}

	public PlyCreator(File xyzrgb,boolean title,List<Area> rr) throws IOException {
		int id=0;
		vertexs=new ArrayList<Vertex>();
		colors=new ArrayList<Color>();
		BufferedReader br=new BufferedReader(new FileReader(xyzrgb));
		if(title)br.readLine();
		String line=null;
		Area area=new Area();
		for(Shape s : rr){
			area.add(new Area(s));
		}
		sp=area;
		while((line=br.readLine())!=null) {
			String[] sp=line.split(",");
			float x=Float.parseFloat(sp[0]);
			float y=Float.parseFloat(sp[1]);
			float z=Float.parseFloat(sp[2]);
			
			int r=Integer.parseInt(sp[3]);
			int g=Integer.parseInt(sp[4]);
			int b=Integer.parseInt(sp[5]);
			Vertex v=new Vertex(x,y,z,id++);
			vertexs.add(v);
			Color c=new Color(r,g,b);
			colors.add(c);
		}
		br.close();
		tin=new IncrementalTin();
		tin.add(vertexs, null);
	}
	
	public PlyCreator(BufferedImage image,BufferedImage dem,AffineTransform af){
		int id=0;
		vertexs=new ArrayList<Vertex>();
		colors=new ArrayList<Color>();
		for(int i=0;i<dem.getWidth();i++){
			for(int j=0;j<dem.getHeight();j++){
				Point2D p=af.transform(new Point2D.Double(i,j),new Point2D.Double());
				double h=PCUtil.getZ(dem.getRGB(i, j));
				if(Double.isNaN(h))continue;
				Vertex v=new Vertex(p.getX(),p.getY(),h,id++);
				vertexs.add(v);
				Color c=new Color(image.getRGB(i, j));
				colors.add(c);
			}
		}
		tin=new IncrementalTin();
		tin.add(vertexs, null);
	}

	public PlyCreator(BufferedImage image,BufferedImage dem,AffineTransform af,List<Shape> r){
		int id=0;
		vertexs=new ArrayList<Vertex>();
		colors=new ArrayList<Color>();
		boolean check=false;
		Area area=new Area();
		for(Shape s : r){
			area.add(new Area(s));
		}
		sp=area;
		for(int i=0;i<dem.getWidth();i++){
			for(int j=0;j<dem.getHeight();j++){
				Point2D p=af.transform(new Point2D.Double(i,j),new Point2D.Double());
				check=false;
				for(Shape s : r){
					if(s.contains(p)){
						check=true;
						break;
					}
				}
				if(!check)continue;
				double h=PCUtil.getZ(dem.getRGB(i, j));
				if(Double.isNaN(h))continue;
				Vertex v=new Vertex(p.getX(),p.getY(),h,id++);
				vertexs.add(v);
				Color c=new Color(image.getRGB(i, j));
				colors.add(c);
			}
		}
		tin=new IncrementalTin();
		tin.add(vertexs, null);
	}

	private void numTri(){
		num_tri=0;
		Consumer<SimpleTriangle> cons=new Consumer<SimpleTriangle>() {
		    @Override
		    public void accept(SimpleTriangle arg){
		    	double x=(arg.getVertexA().x+arg.getVertexB().x+arg.getVertexC().x)/3.0;
		    	double y=(arg.getVertexA().y+arg.getVertexB().y+arg.getVertexC().y)/3.0;
		    	if(sp==null||sp.contains(x, y)){
		    		num_tri++;
		    	}
		    }
		};
		TriangleCollector.visitSimpleTriangles(tin, cons);
	}

	private void outVertex(BufferedWriter bw)throws IOException{
		System.out.println("Vertex出力");
		for(Vertex v : vertexs){
			StringBuffer buf=new StringBuffer();
			buf.append(Float.toString((float)v.getX())+" ");
			buf.append(Float.toString((float)v.getY())+" ");
			buf.append(Float.toString((float)v.getZ())+" ");
			Color c=colors.get(v.getIndex());
			buf.append(Integer.toString(c.getRed())+" ");
			buf.append(Integer.toString(c.getGreen())+" ");
			buf.append(Integer.toString(c.getBlue())+"\n");
			writeBytes(bw,buf.toString());
		}
	}

	private void outFace(BufferedWriter bw)throws IOException{
		System.out.println("Face出力");
		Consumer<SimpleTriangle> cons=new Consumer<SimpleTriangle>() {
		    @Override
		    public void accept(SimpleTriangle arg){
		    	double x=(arg.getVertexA().x+arg.getVertexB().x+arg.getVertexC().x)/3.0;
		    	double y=(arg.getVertexA().y+arg.getVertexB().y+arg.getVertexC().y)/3.0;
		    	if(sp==null||sp.contains(x, y)){
					StringBuffer buf=new StringBuffer();
					buf.append("3");
					buf.append(" "+Integer.toString(arg.getVertexA().getIndex()));
					buf.append(" "+Integer.toString(arg.getVertexB().getIndex()));
					buf.append(" "+Integer.toString(arg.getVertexC().getIndex()));
					buf.append("\n");
					try{
						writeBytes(bw,buf.toString());
					}catch(IOException e){
						e.printStackTrace();
					}
		    	}
		    }
		};
		TriangleCollector.visitSimpleTriangles(tin, cons);
	}

	private void writeBytes(BufferedWriter bw,String str)throws IOException{
		bw.write(str,0,str.length());
	}

	public void writePLY(File f)throws IOException{
		System.out.println("PLY出力");
		Charset charset = Charset.forName("US-ASCII");
		BufferedWriter bw = Files.newBufferedWriter(f.toPath(),charset);
		writeBytes(bw,"ply\n");
		writeBytes(bw,"format ascii 1.0\n");
		writeBytes(bw,"element vertex "+Integer.toString(vertexs.size())+"\n");
		writeBytes(bw,"property float x\n");
		writeBytes(bw,"property float y\n");
		writeBytes(bw,"property float z\n");
		writeBytes(bw,"property uchar red\n");
		writeBytes(bw,"property uchar green\n");
		writeBytes(bw,"property uchar blue\n");
		numTri();
		writeBytes(bw,"element face "+Integer.toString(num_tri)+"\n");
		writeBytes(bw,"property list uchar int vertex_index\n");
		writeBytes(bw,"end_header\n");
		outVertex(bw);
		outFace(bw);
		bw.flush();
		bw.close();
	}
}
