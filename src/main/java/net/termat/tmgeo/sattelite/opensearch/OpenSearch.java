package net.termat.tmgeo.sattelite.opensearch;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenSearch {
	public static String BASE_URL="https://catalogue.dataspace.copernicus.eu/resto/api/collections/{}/search.json?";
	public static final List<String> PRODUCT_LIST=createProductArray();
	
	public static String Sentinel1="Sentinel1";
	public static String Sentinel2="Sentinel2";
	public static String Sentinel3="Sentinel3";
	public static String Sentinel5P="Sentinel5P";
	public static String Sentinel6="Sentinel6";
	public static String Landsat5="Landsat5";
	public static String Landsat7="Landsat7";
	public static String Landsat8="Landsat8";
	public static String COP_DEM="COP-DEM";	//(Copernicus DEM)
	
	public static int maxRecords=10;
	public static int page=1;
	
//	sh-93a2c581-cc94-42b4-aba6-ba24eed2d54e
//	czdcB8T84IdLLEesPfWqJA83xm1CM2Mp
	
	public static void main(String[] args) throws IOException {
//		Rectangle2D r=new Rectangle2D.Double(135.45, 34.65, 0.1, 0.1);
//		String url=createCatalogURL(Sentinel2,"2024-04-01T00:00:00Z","2024-04-30T00:00:00Z",r);
//		System.out.println(url);
//		url=addCloudCover(url,30);
//		url=url+"&productType=S2MSI2A";
//		getCatalog(url);
//		getAccessToken();

		download(
			"https://zipper.dataspace.copernicus.eu/odata/v1/Products(ab4a555f-291a-4ca3-92e0-d5932a4b035d)/$value",
//			"https://catalogue.dataspace.copernicus.eu/download/ab4a555f-291a-4ca3-92e0-d5932a4b035d",
			"S2A_MSIL2A_20240414T013651_N0510_R117_T53SNT_20240414T061952.SAFE",
			"ter.matsuoka@gmail.com",
			"Jm4rpstm#1023$termat"
		);
	}
	
	public static String getKey(String user,String pass) {
		Gson gson=new Gson();
		Map<String, String> map=new HashMap<String,String>();
		map.put("client_id","cdse-public");
		map.put("username", user);
		map.put("password", pass);
		map.put("grant_type","password");
		
		String url="https://identity.dataspace.copernicus.eu/auth/realms/CDSE/protocol/openid-connect/token";
		OkHttpClient client = new OkHttpClient();
		
		RequestBody formBody = new FormBody.Builder().add("client_id","cdse-public").add("username", user).add("password", pass).add("grant_type","password").build();
		Request request = new Request.Builder().url(url).post(formBody).addHeader("Content-Type","application/x-www-form-urlencoded").build();
		try {
			Response response = client.newCall(request).execute();
			@SuppressWarnings("unchecked")
			Map<String,String> ret=gson.fromJson(response.body().string(), Map.class);
			return ret.get("access_token");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void download(String url,String title,String user,String pass) throws IOException {
		OkHttpClient client = new OkHttpClient.Builder().build();
		String token=getKey(user, pass);
		System.out.println(token);
		Request request = new Request.Builder().url(url)
				.addHeader("Authorization", "Bearer "+token).build();

		client.newCall(request).enqueue(new Callback() {
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
			}
			public void onResponse(Call call, Response response) throws IOException {
				if (!response.isSuccessful()) {
					System.out.println("Failed to download file. HTTP error code: " + response.code());
					return;
				}
				try (InputStream in = response.body().byteStream();
						FileOutputStream fos = new FileOutputStream(new File(title+".zip"))) {
					byte[] buffer = new byte[1024];
					int bytesRead;
					long total = 0;
					while ((bytesRead = in.read(buffer)) != -1) {
						total+=bytesRead;
						fos.write(buffer, 0, bytesRead);
					}
					System.out.println(total);
					System.out.println("Downloaded: " + title);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public static void getCatalog(String url,Listener listener) {
		Request request = new Request.Builder().url(url).get().build();
		OkHttpClient client = new OkHttpClient();
		client.newCall(request).enqueue(new Callback() {
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
			}
			public void onResponse(Call call, Response response) {
				try {
					listener.procCatalog(response.body().string());
				} catch (IOException e) {
					listener.error(e);
				}
			}
		});
	}
	
	public static String createCatalogURL(String sat,Date start,Date end,Rectangle2D rect) {
		String url=BASE_URL.replace("{}", sat);
		url=url+getStartDateString(start);
		url=url+"&"+getEndDateString(end);
		url=url+"&"+getBox(rect);	
		return url;
	}
	
	public static String createCatalogURL(String sat,String start,String end,Rectangle2D rect) {
		String url=BASE_URL.replace("{}", sat);
		url=url+"startDate="+start;
		url=url+"&"+"completionDate="+end;
		url=url+"&"+getBox(rect);	
		return url;
	}
	
	public static String addMaxRecord(String base,int maxRecords) {
		return base+"&maxRecords="+Integer.toString(maxRecords);
	}
	
	public static String addCloudCover(String base,int maxPerchent) {
		return base+"&cloudCover=[0,"+Integer.toString(maxPerchent)+"]";
	}
	
	public static String addProductType(String base,String type) {
		return base+"&productType="+type;
	}
	
	private static List<String> createProductArray() {
		List<String> list=new ArrayList<String>();
		list.add("GRD");	//S1
		list.add("RAW");	//S1
		list.add("SLC");	//S1
		list.add("S2MSI1C");	//S2
		list.add("S2MSI2A");	//S2
		list.add("L1G");	//L5
		list.add("L1T");	//L5
		list.add("L1G");	//L7
		list.add("L1GT");	//L7
		list.add("L1T");	//L7	
		list.add("L1GT");	//L8
		list.add("L1T");	//L8
		list.add("L1TP");	//L8
		list.add("L2SP");	//L8
		return list;
	}
	
	public static String getStartDateString(Date date) {
		Calendar cal=Calendar.getInstance();
		cal.setTime(date);
		String ys=Integer.toString(cal.get(Calendar.YEAR));
		String ms=Integer.toString(cal.get(Calendar.MONTH)+1);
		if(ms.length()==1)ms="0"+ms;
		String ds=Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
		if(ds.length()==1)ds="0"+ds;
		return "startDate="+ys+"-"+ms+"-"+ds+"T00:00:00Z";
	}
	
	public static String getEndDateString(Date date) {
		Calendar cal=Calendar.getInstance();
		cal.setTime(date);
		String ys=Integer.toString(cal.get(Calendar.YEAR));
		String ms=Integer.toString(cal.get(Calendar.MONTH)+1);
		if(ms.length()==1)ms="0"+ms;
		String ds=Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
		if(ds.length()==1)ds="0"+ms;
		return "completionDate="+ys+"-"+ms+"-"+ds+"T23:59:59Z";
	}
	
	public static String getCloudCover(int maxPerchent) {
		return "cloudCover[0,"+maxPerchent+"]";
	}
	
	public static String getBox(Rectangle2D r) {
		return "box="+getVal2(r.getX())+","+getVal2(r.getY())+","+getVal2(r.getX()+r.getWidth())+","+getVal2(r.getY()+r.getHeight());
	}
	
	public static String getVal2(double d) {
//		d=Math.round(d*100)/100;
		return Double.toString(d);
	}
	
	public static interface Listener{
		public void procCatalog(String str);
		public void error(Exception e);
	}
}
