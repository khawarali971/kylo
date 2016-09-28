package com.thinkbiganalytics.datalake.authorization.hdfs;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.AclEntryScope;
import org.apache.hadoop.fs.permission.AclEntryType;
import org.apache.hadoop.fs.permission.FsAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Created by Shashi Vishwakarma on 19/9/2016.
 */

public class HDFSUtil 
{

	private static final Logger log = LoggerFactory.getLogger(HDFSUtil.class);

	private static String modelDBPath ="/model.db/";
	private static String appBasePath ="/app/warehouse/";
	private static String etlBasePath ="/etl/";
	private static String archiveBasePath ="/archive/";
	final private static String READ="read";
	final private static String WRITE="write";
	final private static String EXECUTE="execute";
	final private static String ALL="all";
	final private static String NONE="none";


	/**
	 * 
	 * @param configResources : Hadoop configuration resource 
	 * @return
	 * @throws IOException
	 */
	public static Configuration getConfigurationFromResources(String configResources) throws IOException {
		boolean foundResources = false;
		final Configuration config = new Configuration();
		if (null != configResources) {
			String[] resources = configResources.split(",");
			for (String resource : resources) {
				config.addResource(new Path(resource.trim()));
				foundResources = true;
			}
		}

		if (!foundResources) {
			// check that at least 1 non-default resource is available on the classpath
			String configStr = config.toString();
			for (String resource : configStr.substring(configStr.indexOf(":") + 1).split(",")) {
				if (!resource.contains("default") && config.getResource(resource.trim()) != null) {
					foundResources = true;
					break;
				}
			}
		}

		if (!foundResources) {
			throw new IOException("Could not find any of the " + "hadoop conf" + " on the classpath");
		}
		return config;
	}

	/**
	 * 
	 * @param category_name : Category Name
	 * @param feed_name : Feed Name
	 * @param permission_level : Level At Which Permission Needs to be granted.
	 * @return : Returns list of HDFS of Path
	 */
	public String constructResourceforPermissionHDFS(String category_name, String feed_name , String permission_level) {
		// TODO Auto-generated method stub
		String final_resource_path="";

		//Check level at which permission needs to be defined.
		if (permission_level.equalsIgnoreCase("category"))
		{
			String modeldb = modelDBPath + category_name;
			String appPath = appBasePath + category_name;
			String etlPath = etlBasePath + category_name;
			String archivePath = archiveBasePath + category_name;
			final_resource_path = modeldb +","+ appPath + ","+ etlPath + "," + archivePath; 
		}
		else
		{
			String modeldb = modelDBPath + category_name + "/" +feed_name;
			String appPath = appBasePath + category_name + "/" + feed_name;
			String etlPath = etlBasePath + category_name + "/" + feed_name ;
			String archivePath = archiveBasePath + category_name + "/" +feed_name;
			final_resource_path = modeldb +","+ appPath + ","+ etlPath + "," + archivePath; 
		}
		return final_resource_path;
	}

	/**
	 * 
	 * @param allPathForAclCreation : Each Kylo internal path
	 * @param conf : Hadoop configuration resources
	 * @param fileSystem : HDFS filesystem 
	 * @param groupList : List of group for granting permission
	 * @param hdfs_permission 
	 * @throws IOException
	 */
	public void splitPathListAndApplyPolicy(String allPathForAclCreation , Configuration conf , FileSystem fileSystem, String groupList, String hdfs_permission) throws IOException
	{
		String allKyloIntermediatePath[] = allPathForAclCreation.split(",");
		for(int pathCounter = 0 ; pathCounter < allKyloIntermediatePath.length ; pathCounter ++)
		{
			try
			{
				individualIntermediatePath(conf,fileSystem,allKyloIntermediatePath[pathCounter] , groupList ,hdfs_permission);
			}
			catch (IOException e)
			{
				throw new IOException("Unable to iterate on HDFS directories " + e.getMessage());
			} 
		}
	}

	public void individualIntermediatePath(Configuration conf , FileSystem fileSystem , String kyloPath, String groupList , String hdfs_permission ) throws IOException 
	{
		Path path = new Path(kyloPath);
		fileSystem = path.getFileSystem(conf);
		listAllDir(fileSystem ,path , groupList ,hdfs_permission);
	}

	/**
	 * 
	 * @param fileSystem : HDFS fileSystem object
	 * @param path : Path on which ACL needs to be created 
	 * @param groupList : List of group to which permission needs to be granted.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */

	public void listAllDir(FileSystem fileSystem ,Path path ,String groupList , String hdfs_permission) throws FileNotFoundException, IOException
	{
		FsAction fsActionObject = getFinalPermission(hdfs_permission);
		FileStatus[] fileStatus = fileSystem.listStatus(path);
		
		for(FileStatus status : fileStatus)
		{
			/**
			 * Flush ACL before creating new one.
			 */

			flushAcl(fileSystem, status.getPath());


			/**
			 * Apply ACL recursively on each file/directory.
			 */

			if(status.isDirectory())
			{
				String groupListForPermission[] = groupList.split(",");
				for(int groupCounter = 0 ; groupCounter < groupListForPermission.length ; groupCounter ++)
				{

					/**
					 * Create HDFS ACL for each for each Path on HDFS
					 */


					AclEntry aclEntry = new AclEntry.Builder().setName(groupListForPermission[groupCounter]) 
							.setPermission(fsActionObject).setScope(AclEntryScope.ACCESS).setType(AclEntryType.GROUP).build();

					/**
					 * Apply ACL on Path
					 */

					applyAcl(fileSystem,status.getPath(),aclEntry);

				}

				/**
				 * Recursive call made to apply acl on each sub directory
				 */
				listAllDir(fileSystem,status.getPath(), groupList , hdfs_permission);
			}
			else
			{
				String groupListForPermission[] = groupList.split(",");
				for(int groupCounter = 0 ; groupCounter < groupListForPermission.length ; groupCounter ++)
				{

					/**
					 * Create HDFS ACL for each for each Path on HDFS
					 */

					AclEntry aclEntry = new AclEntry.Builder().setName(groupListForPermission[groupCounter])
							.setPermission(fsActionObject).setScope(AclEntryScope.ACCESS).setType(AclEntryType.GROUP).build();

					/**
					 * Apply ACL on Path
					 */

					applyAcl(fileSystem,status.getPath(),aclEntry);

				}
			}
		}
	}

	/**
	 * 
	 * @param fileSystem : HDFS FileSystem Object
	 * @param path : HDFS Path 
	 * @param aclEntry : ACL for HDFS Path
	 * @throws IOException 
	 */
	public void applyAcl(FileSystem fileSystem , Path path , AclEntry aclEntry) throws IOException
	{
		try
		{
			log.info("Creating ACL for Path - " + path.toString());
			fileSystem.modifyAclEntries(path, Lists.newArrayList(aclEntry));
			log.info("Sucessfully created ACL for Path - " + path.toString());

		} catch (IOException e) 
		{
			throw new IOException("Unable to apply HDFS Policy for " +path.toString() + " " +e.getMessage());
		}
	}

	/**
	 * 
	 * @param fileSystem : HDFS FileSystem Object
	 * @param path : HDFS Path 
	 * @throws IOException
	 */
	public void flushAcl(FileSystem fileSystem , Path path) throws IOException
	{
		try
		{
			fileSystem.removeAcl(path);

		} catch (IOException e) 
		{
			throw new IOException("Unable to flush HDFS Policy for " +path.toString() + " " +e.getMessage());
		}

	}

	/**
	 * 
	 * @param hdfsPermission : Permission assgined by user.
	 * @return : Final Permission to be set for creating ACL 
	 */
	
	private FsAction getFinalPermission(String hdfsPermission)
	{

		HashMap<String,Integer> standardPermissionMap=new HashMap<String,Integer>(); 

		String  permissions[] =  hdfsPermission.split(",");
		standardPermissionMap.put(READ, 0);
		standardPermissionMap.put(WRITE, 0);
		standardPermissionMap.put(EXECUTE, 0);
		standardPermissionMap.put(NONE,0 );
		standardPermissionMap.put(ALL, 0);

		for ( String permission : permissions){

			permission = permission.toLowerCase();
			
			switch (permission)
			{
			case READ: standardPermissionMap.put(READ, 1);break ;
			case WRITE :standardPermissionMap.put(WRITE, 1);break ;
			case EXECUTE : standardPermissionMap.put(EXECUTE, 1); break ;
			case ALL: return FsAction.ALL; 
			case NONE : return FsAction.NONE ; 
			default : standardPermissionMap.put(NONE, 1 ) ;
			}
		}

			if (standardPermissionMap.get(READ)==1 && standardPermissionMap.get(WRITE)==1 && standardPermissionMap.get(EXECUTE)==1)
				return FsAction.ALL ;

			if (standardPermissionMap.get(READ)==1 && standardPermissionMap.get(WRITE)==1)
			{
				return FsAction.READ_WRITE ;
			}

			if (standardPermissionMap.get(READ)==1 && standardPermissionMap.get(EXECUTE)==1)
			{
				return FsAction.READ_EXECUTE;
			}

			if (standardPermissionMap.get(WRITE)==1 && standardPermissionMap.get(EXECUTE)==1)
			{
				return FsAction.WRITE_EXECUTE;
			}

			if (standardPermissionMap.get(WRITE)==1 )
			{
				return FsAction.WRITE;
			}

			if (standardPermissionMap.get(READ)==1 )
			{
				return FsAction.READ;
			}

			if (standardPermissionMap.get(EXECUTE)==1 )
			{
				return FsAction.EXECUTE;
			}
		

		/**
		 * Default Permission - None
		 */
		return FsAction.NONE;

	}
}
