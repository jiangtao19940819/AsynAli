package com.baiwang.AsynAli;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Properties;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
public class MHRed {
	   private String filePath;
	   private String id;
	   private String mc;
	   private String nsrsbh;
	   private String token;
	   private String lsh;
	   private String qzUrl;
	   private String ansySKURL;
	   private String kpzdbs;
	   private static Logger logger = Logger.getLogger(MHRed.class.getName());
	public static void main(String[] args) throws Exception{
		MHRed mr = new MHRed();
		mr.init();
		int num = 0;
		List<List<File>> alist = mr.getFile();
		for(List<File> flist:alist){
			if(!mr.cheakall(flist)){
				num++;
			}
		}
		if(num>0){
			logger.info("总共有"+num+"开票错误");
		}else{
			logger.info("全部开票失败");
		}
	}
	public boolean cheakall(List<File> fList) throws Exception{
		boolean flag = false;
		String bluebw = getBaowen(fList.get(1));
		//String redbw = getBaowen(fList.get(0));
		if(bluekp(bluebw)){
			Thread.sleep(5000);
			String result = getFp();
			if(result.contains("<returncode>0</returncode>")){
				logger.info(fList.get(1).getName().substring(0,fList.get(1).getName().indexOf("."))+": 开票成功");
				String[] arg = getDH(result);
				String fpdm = arg[0];
				String fphm = arg[1];
				String redbw = getBaowen(fList.get(0));
				if(redkp(redbw,fpdm,fphm)){
					Thread.sleep(5000);
					String re = getFp();
					if(re.contains("<returncode>0</returncode>")){
						logger.info(fList.get(0).getName().substring(0,fList.get(0).getName().indexOf("."))+": 开票成功");
						flag = true;
					}else{
						logger.info(fList.get(0).getName().substring(0,fList.get(0).getName().indexOf("."))+": 开票失败");
						return flag;
					}
				}else{
					logger.info(fList.get(0).getName().substring(0,fList.get(0).getName().indexOf("."))+": 放入mq失败");
					return flag;
				}
			}else{
				logger.info(fList.get(1).getName().substring(0,fList.get(1).getName().indexOf("."))+": 开票失败");
				return flag;
			}
			
		}else{
			logger.info(fList.get(1).getName().substring(0,fList.get(1).getName().indexOf("."))+": 放入mq失败");
			return flag;
		}
		return flag;
	}
	public String[] getDH(String re) throws Exception{
		SAXBuilder build = new SAXBuilder();
		InputStream is = new ByteArrayInputStream(re.getBytes());
		Document document = build.build(is);
		Element root = document.getRootElement();
		Element body = root.getChild("body");
		Element returndata = body.getChild("returndata");
		Element kpxx = returndata.getChild("kpxx");
		Element group = kpxx.getChild("group");
		String fpdm = group.getChildText("fpdm");
		String fphm = group.getChildText("fphm");
		String[] arg = {fpdm,fphm};
		logger.info(fpdm+"_"+fphm);
		return arg;
		
	}
	public String getFp() throws Exception{
		   CloseableHttpClient client = HttpClients.createDefault();
		   HttpPost dpo =  new HttpPost(ansySKURL);
		   String bw = "<?xml version=\"1.0\" encoding=\"gbk\"?><business id=\"30012\" comment=\"发票查询\"><body yylxdm=\"1\"><kpzdbs>"+kpzdbs+"</kpzdbs><fplxdm>026</fplxdm><cxfs>2</cxfs><cxtj>"+lsh+"</cxtj></body></business>";
		   //logger.info("1111"+bw);
		   StringEntity entry = new StringEntity(bw);
		   dpo.setEntity(entry);
		   HttpResponse re = client.execute(dpo);
		   String result = EntityUtils.toString(re.getEntity());
		   logger.info(result);
		   /*if(result.contains("<returncode>0</returncode>")){
			   flag = true;
		   }*/
		   client.close();
		   return result;
	   }
	public boolean redkp(String baowen,String fpdm,String fphm) throws Exception{
		   boolean flag = false;
		   baowen = baowen.replace("{{FPDM}}",fpdm);
		   baowen = baowen.replace("{{FPHM}}",fphm);
		   //logger.info(baowen);
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
	 public boolean bluekp(String baowen) throws Exception{
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
	public List<List<File>> getFile(){
		String path = filePath + File.separator +"蓝红报文";
		File file = new File(path);
		List<List<File>> flist = new ArrayList<List<File>>();
		if(file.exists()){
			File[] afileList = file.listFiles();
			List<File> fileList = new ArrayList<File>();
			for(File f:afileList){
				fileList.add(f);
			}
			int num = fileList.size()/2;
			for(int i=0;i<num;i++){
				List<File> alist = new ArrayList<File>();
				alist.add(fileList.get(i));
				for(int a=i+1;a<fileList.size();a++){
					if(fileList.get(i).getName().substring(0,2).equals(fileList.get(a).getName().substring(0,2))){
						alist.add(fileList.get(a));
						fileList.remove(a);
						flist.add(alist);
						break;
					}
				}
			}
			System.out.println(flist);
			
		}else{
			logger.info("文件路径不正确");
		}
		return flist;
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
	   public String getLSH(){
		   String str = "jt22";
		   for(int i=0;i<15;i++){
			   int num = (int)(Math.random()*10);
			   str+=num;
		   }
		   return str;
	   }
}
