package net.termat.tmgeo.fomat.ply;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlyInsertPoint {
	private int num_vert;
	private File file;
	private List<Pos> top=new ArrayList<Pos>();
	private List<Pos> crown=new ArrayList<Pos>();
/*
	public static void main(String[] args){
		File ply=new File("D:/Data/静岡PCDB/30XXX00010014/30XXX00010014_DEM_XYZRGB_0.4.ply");
		PlyInsertTreePoint app=new PlyInsertTreePoint(ply);
//		app.addTop(new File("C:/Dropbox (株式会社ウエスコ)/赤磐市森林/3Dモデル/樹頂点樹冠/林班DSM_Peak01.txt"));
		app.addCrown(new File("D:/Data/静岡PCDB/30XXX00010014/30XXX00010014_PearkAndCrown.txt"));
		File out=new File("D:/Data/静岡PCDB/30XXX00010014/30XXX00010014_DEM_XYZRGB_0.4TREE.ply");
		try{
			app.out(out);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
*/
	public PlyInsertPoint(File ply){
		file=ply;
		try{
			BufferedReader br=new BufferedReader(new FileReader(file));
			String line=null;
			while((line=br.readLine())!=null){
				if(line.contains("element vertex")){
					String[] ss=line.split(" ");
					num_vert=Integer.parseInt(ss[2]);
				}else if(line.contains("end_header")){
					break;
				}
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public void out(File f,Color color)throws IOException{
		String col=" "+color.getRed()+" "+color.getGreen()+" "+color.getBlue();
		int num=num_vert+top.size()+crown.size();
		BufferedReader br=new BufferedReader(new FileReader(file));
		BufferedWriter bw=new BufferedWriter(new FileWriter(f));
		String line=null;
		while((line=br.readLine())!=null){
			if(line.contains("element vertex")){
				String nl="element vertex "+Integer.toString(num);
				bw.write(nl+"\n");
			}else if(line.contains("end_header")){
				bw.write(line+"\n");
				for(int i=0;i<num_vert;i++){
					line=br.readLine();
					bw.write(line+"\n");
				}
				for(Pos p :top){
					String n0=Double.toString(p.x)+" "+Double.toString(p.y)+" "+Double.toString(p.z)+col;
					bw.write(n0+"\n");
				}
				for(Pos p :crown){
					String n0=Double.toString(p.x)+" "+Double.toString(p.y)+" "+Double.toString(p.z)+col;
					bw.write(n0+"\n");
				}
			}else{
				bw.write(line+"\n");
			}
		}
		br.close();
		bw.close();
	}

	public void addCrown(File f){
		try{
			BufferedReader br=new BufferedReader(new FileReader(f));
			String line=null;
			while((line=br.readLine())!=null){
				String[] ss=line.split(" ");
				double x=Double.parseDouble(ss[0]);
				double y=Double.parseDouble(ss[1]);
				double z=Double.parseDouble(ss[2]);
				crown.add(new Pos(x,y,z));
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public void addTop(File f){
		try{
			BufferedReader br=new BufferedReader(new FileReader(f));
			String line=null;
			while((line=br.readLine())!=null){
				String[] ss=line.split(" ");
				double x=Double.parseDouble(ss[0]);
				double y=Double.parseDouble(ss[1]);
				double z=Double.parseDouble(ss[2]);
				top.add(new Pos(x,y,z));
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	private class Pos{
		double x;
		double y;
		double z;

		public Pos(double x,double y,double z){
			this.x=x;
			this.y=y;
			this.z=z;
		}

	}

}
