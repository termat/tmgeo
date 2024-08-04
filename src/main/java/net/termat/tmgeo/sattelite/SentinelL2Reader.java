package net.termat.tmgeo.sattelite;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.termat.tmgeo.data.BandReader;

public class SentinelL2Reader {
	public static final String L2_10m=".*10m.jp2";
	public static final String L2_20m=".*20m.jp2";
	public static final String L2_TRUE=".*_TCI_10m.jp2";

	public static void createS2Tif(File f,String tag,File outDir) throws IOException {
		SentinelL2Reader app=new SentinelL2Reader();
		app.readZip(f, tag,outDir);
	}
	
	public static void createS2Tif(File f,String tag) throws IOException {
		File outDir=f.getParentFile();
		SentinelL2Reader app=new SentinelL2Reader();
		app.readZip(f, tag,outDir);
	}
	
	public void readZip(File f,String ext,File outDir) throws IOException {
		ZipFile zipFile = new ZipFile(f);
		Consumer<ZipEntry> cons = entry -> {
			String name=entry.getName();
			if(!name.matches(ext))return;
			try (final var is = zipFile.getInputStream(entry)) {
				final var bytes = is.readAllBytes();
				Path tmpPath = new File(outDir.getAbsolutePath()+"/"+getName(name)).toPath();
				Files.write(tmpPath,bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		};
		zipFile.stream().forEach(cons);
		zipFile.close();
	}

	
	private static String getName(String name) {
		String[] ss=name.replace("\\","/").split("/");
		return ss[ss.length-1];
	}
	
	public static void main(String[] args) throws IOException {
		File f=new File("G:\\衛星\\NEDO\\qgis\\sentinel2\\S2A_MSIL2A_20240315T013651_N0510_R117_T53SNU_20240315T050951.SAFE.zip");
		createS2Tif(f,L2_TRUE,new File("G:\\衛星\\NEDO\\qgis\\sentinel2"));
	}
	
	
	public static BufferedImage tcl2Bi(BandReader br) {
		float[][] r=br.getBand(0);
		float[][] g=br.getBand(1);
		float[][] b=br.getBand(2);
		BufferedImage ret=new BufferedImage(r.length,r[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<r.length;i++) {
			for(int j=0;j<r[0].length;j++) {
				Color c=new Color(
						(int)r[i][j],
						(int)g[i][j],
						(int)b[i][j]);
				ret.setRGB(i, j, c.getRGB());
			}
		}
		return ret;
	}
}
