package net.termat.tmgeo.fomat.ply;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PLYFileParser {
	private int numVert;
	private int numFace;
	private List<XyzRgb> vert=new ArrayList<XyzRgb>();
	private List<Face> face=new ArrayList<Face>();

	public PLYFileParser(File ply){
		try{
			BufferedReader br=new BufferedReader(new FileReader(ply));
			String line=null;
			while((line=br.readLine())!=null){
				if(line.contains("element vertex")){
					String[] ss=line.split(" ");
					numVert=Integer.parseInt(ss[2]);
				}else if(line.contains("element face")){
					String[] ss=line.split(" ");
					numFace=Integer.parseInt(ss[2]);
				}else if(line.contains("end_header")){
					for(int i=0;i<numVert;i++){
						line=br.readLine();
						vert.add(parseVertex(line));
					}
					for(int i=0;i<numFace;i++){
						line=br.readLine();
						face.add(parseFace(line));
					}
				}
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public void vertexCentred(){
		double xmin=Double.MAX_VALUE;
		double xmax=-Double.MAX_VALUE;
		double ymin=Double.MAX_VALUE;
		double ymax=-Double.MAX_VALUE;
		double zmin=Double.MAX_VALUE;
		double zmax=-Double.MAX_VALUE;
		for(XyzRgb v : vert){
			xmin=Math.min(xmin, v.x);
			xmax=Math.max(xmax, v.x);
			ymin=Math.min(ymin, v.y);
			ymax=Math.max(ymax, v.y);
			zmin=Math.min(zmin, v.z);
			zmax=Math.max(zmax, v.z);
		}
		float xc=(float)(xmin+xmax)/2;
		float yc=(float)(ymin+ymax)/2;
		float zc=(float)zmin;
		for(XyzRgb v : vert){
			v.x=v.x-xc;
			v.y=v.y-yc;
			v.z=v.z-zc;
		}
	}

	public void join(PLYFileParser ply){
		int tmp=vert.size();
		this.vert.addAll(ply.vert);
		for(Face f:ply.face){
			f.p1 +=tmp;
			f.p2 +=tmp;
			f.p3 +=tmp;
			this.face.add(f);
		}
	}

	public void changeYZ(){
		for(XyzRgb p : vert){
			float z=p.y;
			float y=p.z;
			p.y=y;
			p.z=z;
		}
	}

	public void out(File out)throws IOException{
		Charset charset = Charset.forName("US-ASCII");
		BufferedWriter bw = Files.newBufferedWriter(out.toPath(),charset);
		writePLYHeader(bw,vert.size(),face.size());
		for(XyzRgb v : vert){
			StringBuffer buf=new StringBuffer();
			buf.append(Float.toString(v.x)+" ");
			buf.append(Float.toString(v.y)+" ");
			buf.append(Float.toString(v.z)+" ");
			buf.append(Integer.toString(v.r)+" ");
			buf.append(Integer.toString(v.g)+" ");
			buf.append(Integer.toString(v.b)+"\n");
			writeBytes(bw,buf.toString());
		}
		for(Face f : face){
			StringBuffer buf=new StringBuffer();
			buf.append(Integer.toString(f.n)+" ");
			buf.append(Integer.toString(f.p1)+" ");
			buf.append(Integer.toString(f.p2)+" ");
			buf.append(Integer.toString(f.p3)+"\n");
			writeBytes(bw,buf.toString());
		}
		bw.flush();
		bw.close();
	}

	private static void writePLYHeader(BufferedWriter bw,int verNum,int faceNum)throws IOException{
		writeBytes(bw,"ply\n");
		writeBytes(bw,"format ascii 1.0\n");
		writeBytes(bw,"element vertex "+Integer.toString(verNum)+"\n");
		writeBytes(bw,"property float x\n");
		writeBytes(bw,"property float y\n");
		writeBytes(bw,"property float z\n");
		writeBytes(bw,"property uchar red\n");
		writeBytes(bw,"property uchar green\n");
		writeBytes(bw,"property uchar blue\n");
		writeBytes(bw,"element face "+Integer.toString(faceNum)+"\n");
		writeBytes(bw,"property list uchar int vertex_index\n");
		writeBytes(bw,"end_header\n");
	}

	private static void writeBytes(BufferedWriter bw,String str)throws IOException{
		bw.write(str,0,str.length());
	}

	private XyzRgb parseVertex(String line){
		String[] p=line.split(" ");
		XyzRgb ret=new XyzRgb();
		ret.x=Float.parseFloat(p[0]);
		ret.y=Float.parseFloat(p[1]);
		ret.z=Float.parseFloat(p[2]);
		ret.r=Integer.parseInt(p[3]);
		ret.g=Integer.parseInt(p[4]);
		ret.b=Integer.parseInt(p[5]);
		return ret;
	}

	private Face parseFace(String line){
		String[] p=line.split(" ");
		Face ret=new Face();
		ret.n=Integer.parseInt(p[0]);
		ret.p1=Integer.parseInt(p[1]);
		ret.p2=Integer.parseInt(p[2]);
		ret.p3=Integer.parseInt(p[3]);
		return ret;
	}

	private class XyzRgb{
		float x;
		float y;
		float z;
		int r;
		int g;
		int b;
	}

	private class Face{
		int n;
		int p1;
		int p2;
		int p3;
	}

	public static void main(String[] args){
		File in0=new File("C:/Users/t-matsuoka/Desktop/赤磐/akaiwa0.50DEM_rinpan01.ply");
		PLYFileParser fp1=new PLYFileParser(in0);
		File in1=new File("C:/Users/t-matsuoka/Desktop/赤磐/TREE01.ply");
		PLYFileParser fp2=new PLYFileParser(in1);
		fp1.join(fp2);
		File out=new File("C:/Users/t-matsuoka/Desktop/赤磐/akaiwa0.50DEM_TREE01.ply");
//		fp.changeYZ();
		try{
			fp1.out(out);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
