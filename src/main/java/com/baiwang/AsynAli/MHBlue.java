package com.baiwang.AsynAli;
import java.io.File;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
public class MHBlue {
   private String filePath;
   private String id;
   private String mc;
   private String nsrsbh;
   private String token;
   private String lsh;
   private String qzUrl;
   private String ansySKURL;
   private String kpzdbs;
   private static Logger logger = Logger.getLogger(MHBlue.class.getName());
   public static void main(String[] args) throws Exception{
	   MHBlue blue = new MHBlue();
	   blue.init();
	   File[] fileList = blue.getFile();
	   int a = 0;
	   for(File f:fileList) {
		   String fileName = f.getName().substring(0,f.getName().indexOf("."));
		   String baowen = blue.getBaowen(f);
		   if(blue.kp(baowen)) {
			   Thread.sleep(5000);
			   if(blue.getFp()) {
				   
				   logger.info(fileName+": 开票成功");
			   }else {
				   logger.info(fileName+": 开票失败");
				   a++;
			   }
		   }else {
			   logger.info(fileName+": 放入Mq失败");
			   a++;
			   continue;
		   }
	   }
	   if(a>0) {
		   logger.info("总共有"+a+"开票失败");
	   }else {
		   logger.info("全部开票成功");
	   }
   }
   public boolean kp(String baowen) throws Exception{
	   boolean flag = false;
	   CloseableHttpClient client = HttpClients.createDefault();
	   String api = qzUrl+"/einvoice/handle/save";
	   HttpPost dp = new HttpPost(api);
	   StringEntity entry = new StringEntity(baowen,"utf-8");
	   dp.setEntity(entry);
	   dp.addHeader("bwts",token);
	   HttpResponse res = client.execute(dp);
	   String re = EntityUtils.toString(res.getEntity());
	   if(re.contains("<returncode>4000</returncode>")){
		   flag = true;
	   }
	   //logger.info(re);
	   client.close();
	   return flag;
   }
   public boolean getFp() throws Exception{
	   boolean flag = false;
	   CloseableHttpClient client = HttpClients.createDefault();
	   HttpPost dpo =  new HttpPost(ansySKURL);
	   String bw = "<?xml version=\"1.0\" encoding=\"gbk\"?><business id=\"30012\" comment=\"发票查询\"><body yylxdm=\"1\"><kpzdbs>"+kpzdbs+"</kpzdbs><fplxdm>026</fplxdm><cxfs>2</cxfs><cxtj>"+lsh+"</cxtj></body></business>";
	   //logger.info("1111"+bw);
	   StringEntity entry = new StringEntity(bw);
	   dpo.setEntity(entry);
	   HttpResponse re = client.execute(dpo);
	   String result = EntityUtils.toString(re.getEntity());
	   logger.info(result);
	   if(result.contains("<returncode>0</returncode>")){
		   flag = true;
	   }
	   client.close();
	   return flag;
   }
   public void init() throws Exception{
	   Properties prop = new Properties();
	   //FileInputStream fis = new FileInputStream("properties.properties");
	   InputStreamReader is = new InputStreamReader(new FileInputStream("properties.properties"));
	   prop.load(is);
	   filePath = prop.getProperty("filepath");
	   id = prop.getProperty("id");
	   mc= prop.getProperty("mc");
	   //logger.info(mc);
	   nsrsbh = prop.getProperty("nsrsbh");
	   token = prop.getProperty("token");
	   qzUrl = prop.getProperty("qzURL");
	   ansySKURL = prop.getProperty("ansySKURL");
	   kpzdbs = prop.getProperty("kpzdbs");
   }
   public String getBaowen(File file) throws Exception{
	   InputStreamReader is = new InputStreamReader(new FileInputStream(file),"gbk");
	   BufferedReader br = new BufferedReader(is);
	   String str;
	   String baowen = "";
	   lsh = getLSH();
	   while((str=br.readLine())!=null){
		   baowen+=str.trim();
	   }
	   baowen = baowen.replace("{{ID}}",id);
	   baowen = baowen.replace("{{MC}}",mc);
	   baowen = baowen.replace("{{NSRSBH}}",nsrsbh);
	   baowen = baowen.replace("{{LSH}}",lsh);
	   //logger.info(baowen);
	   br.close();
	   return baowen;
   }
   public File[] getFile(){
	   File[] fileList = null;
	   String tsbw = filePath+File.separator+"特殊报文";
	   File file = new File(tsbw);
	   if(file.exists()){
		    fileList = file.listFiles();
	   }else{
		   logger.info("文件路径错误");
	   }
	  /* for(File f:fileList){
		   logger.info(f.toString());
	   }*/
	 return fileList;  
   }
   public String getLSH(){
	   String str = "jt22";
	   for(int i=0;i<15;i++){
		   int num = (int)(Math.random()*10);
		   str+=num;
	   }
	   return str;
   }
}
