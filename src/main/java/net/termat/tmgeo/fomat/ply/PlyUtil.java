package net.termat.tmgeo.fomat.ply;

//import net.termat.geo.pointcloud.hull3d.Delaunay3D;
//import net.termat.geo.pointcloud.peakfinder.Point3d;

public class PlyUtil {
/*
	public static void xyzrgbToPly(File xyz,String separator,File out)throws IOException{
		PlyCreator pc=new PlyCreator();
		pc.readXYZGRB(xyz, separator);
		pc.writePLY(out);
	}

	public static void xyzrgbToPly(File xyz,File geojson,String separator,File out)throws IOException{
		List<Shape> sp=PlyCreator.readGeoJson(geojson);
		PlyCreator pc=new PlyCreator(sp.get(0));
		pc.readXYZGRB(xyz, separator);
		pc.writePLY(out);
	}

	public static void plyInsertPoint(File ply,File point,Color pointColor,File out)throws IOException{
		PlyInsertPoint pp=new PlyInsertPoint(ply);
		pp.addCrown(point);
		pp.out(out, pointColor);
	}

	public static void crownGroupToPly(File peakf,File out,Color col)throws Exception{
		String color=" "+col.getRed()+" "+col.getGreen()+" "+col.getBlue();
    	List<Point3d> vert=new ArrayList<Point3d>();
    	List<int[]> face=new ArrayList<int[]>();
    	Map<Integer,List<Point3d>> map=new HashMap<Integer,List<Point3d>>();
    	BufferedReader br=new BufferedReader(new FileReader(peakf));
    	String line=null;
    	int id=0;
    	while((line=br.readLine())!=null){
    		String[] p=line.split(" ");
    		int gp=Integer.parseInt(p[0]);
    		double x=Double.parseDouble(p[1]);
    		double y=Double.parseDouble(p[2]);
    		double z=Double.parseDouble(p[3]);
    		Point3d v=new Point3d(x,y,z,id++);
    		if(map.containsKey(gp)){
    			map.get(gp).add(v);
    		}else{
    			List<Point3d> l=new ArrayList<Point3d>();
    			l.add(v);
    			map.put(gp, l);
    		}
    	}
    	br.close();
    	Integer[] ii=map.keySet().toArray(new Integer[map.keySet().size()]);
    	Arrays.sort(ii);
    	for(int iv : ii){
    		List<Point3d> ll=map.get(iv);
    		Delaunay3D del=Delaunay3D.build(ll);
    		List<int[]> fl=del.getSurface();
    		for(int[] fx : fl){
    			int[] ppp=new int[]{
    				ll.get(fx[0]).getIndex(),
    				ll.get(fx[1]).getIndex(),
    				ll.get(fx[2]).getIndex()};
    			face.add(ppp);
    		}
    		vert.addAll(ll);
    	}
		Charset charset = Charset.forName("US-ASCII");
		BufferedWriter bw = Files.newBufferedWriter(out.toPath(),charset);
		writePLYHeader(bw,vert.size(),face.size());
		for(Point3d v : vert){
			StringBuffer buf=new StringBuffer();
			buf.append(Float.toString((float)v.getX())+" ");
			buf.append(Float.toString((float)v.getY())+" ");
			buf.append(Float.toString((float)v.getZ()));
			buf.append(color+"\n");
			writeBytes(bw,buf.toString());
		}
		for(int[] f : face){
			StringBuffer buf=new StringBuffer();
			buf.append(Integer.toString(f.length));
			buf.append(" "+Integer.toString(f[0]));
			buf.append(" "+Integer.toString(f[1]));
			buf.append(" "+Integer.toString(f[2]));
			buf.append("\n");
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

	public static void main(String[] args){
		try{
			File in=new File("C:/Users/t-matsuoka/Desktop/赤磐/林班Peak01.txt");
			File out=new File("C:/Users/t-matsuoka/Desktop/赤磐/TREE01.ply");
			PlyUtil.crownGroupToPly(in, out, Color.green);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	*/
}
