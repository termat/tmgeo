package net.termat.tmgeo.fomat.ply;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class XYZRGB2PLY {

	public static void main(String[] args) throws IOException {
		File dir=new File("H:\\仕事\\R06市原市\\ply");
		for(File f : dir.listFiles()) {
			if(f.getName().toLowerCase().endsWith(".xyz")) {
				System.out.println(f.getName());
				XYZ2PLY(f,",",new File(f.getAbsolutePath().replace(".xyz", ".ply")));
			}
		}
	}
	
	public static void XYZ2PLY(File xyzrgb,String separator,File out) throws IOException {
		System.out.println("RGBXYZチェック");
		int n=checkRow(xyzrgb);
		System.out.println("PLY出力");
		Charset charset = Charset.forName("US-ASCII");
		BufferedWriter bw = Files.newBufferedWriter(out.toPath(),charset);
		writeBytes(bw,"ply\n");
		writeBytes(bw,"format ascii 1.0\n");
		writeBytes(bw,"element vertex "+Integer.toString(n)+"\n");
		writeBytes(bw,"property float x\n");
		writeBytes(bw,"property float y\n");
		writeBytes(bw,"property float z\n");
		writeBytes(bw,"property uchar red\n");
		writeBytes(bw,"property uchar green\n");
		writeBytes(bw,"property uchar blue\n");
		writeBytes(bw,"end_header\n");
		outVertex(xyzrgb,bw,separator);
		bw.flush();
		bw.close();
	}
	
	private static void outVertex(File f,BufferedWriter bw,String sp)throws IOException{
		BufferedReader br=new BufferedReader(new FileReader(f));
		String line=null;
		int n=0;
		while((line=br.readLine())!=null) {
			if(n++%10000==0)System.out.println(n);
			String[] p=line.split(sp);
			StringBuffer buf=new StringBuffer();
			buf.append(Float.toString(Float.parseFloat(p[0]))+" ");
			buf.append(Float.toString(Float.parseFloat(p[1]))+" ");
			buf.append(Float.toString(Float.parseFloat(p[2]))+" ");
			buf.append(p[3]+" ");
			buf.append(p[4]+" ");
			buf.append(p[5]+"\n");
			writeBytes(bw,buf.toString());
		}
		br.close();
	}
	
	private static void writeBytes(BufferedWriter bw,String str)throws IOException{
		bw.write(str,0,str.length());
	}
	
	private static int checkRow(File f) throws IOException {
		int n=0;
		BufferedReader br=new BufferedReader(new FileReader(f));
		while(br.readLine()!=null) {
			n++;
		}
		br.close();
		return n;
	}
}
