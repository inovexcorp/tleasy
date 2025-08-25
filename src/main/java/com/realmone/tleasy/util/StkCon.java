/**
 * NOTE: This class is provided by STK to allow you to send commands through STK's Connect Command CLI.
 * More information on this class and how to use it can be found here:
 * https://help.agi.com/stkdevkit/index.htm#../Subsystems/connectCmds/Content/commandListings.htm?TocPath=Library%2520Reference%257CConnect%2520Command%2520Library%257C_____0
 */

package com.realmone.tleasy.util;

import java.net.*;
import java.io.*;
import java.util.*;

public class StkCon {
    //Variables

    protected Socket         socket         = null;
    protected String         host           = "localhost";
    protected int            port           = 5001;
    protected PrintWriter    toStk          = null;
    protected BufferedReader fromStk        = null;
    protected Hashtable<String, String>      returnDataHash = null;
    protected StringBuffer   retStringBuffer;
    protected boolean returnedAck;
    protected boolean        ack            = false;
    protected boolean        async          = false;





    //Methods

    /*Name:StkCon
     *Returns:  None
     *Arguments: None
     *Description: Standard constructor, sets up connection to STK on localhost:5001
     *Modifications: None
     *Version: 1.1
     */
    public StkCon(){
        host="localhost";
        port=5001;
        initVariables();
    }





    /*Name:StkCon
     *Returns:  None
     *Arguments: String Host, int Port#
     *Description: constructor which lets the user create a connection on any
                    machine and port, but passes in as separate parameters.
     *Modifications: 1.1  Moved variable initialization outside of the function to
                        new method "initVariables" to allow for code reuse
                        among constructors.
     *Version: 1.0,1.1
     *
     */
    public StkCon(String h, int p) {
        host = h;
        port = p;
        initVariables();
    }





    /*Name:StkCon
     *Returns:  None
     *Arguments: String connectionInfo
     *Description: constructor which lets the user create a connection on any
                    machine and port. Data is passed in with a string of the
                    following format: "<machine>:<port>" so that if one were
                    connecting to port 5001 on a machine named "artemis" the
                    string would look like: "artemis:5001"
     *Modifications: None
     *Version: 1.1
     */
    public StkCon(String connectionInfo) {
        StringTokenizer st=new StringTokenizer(connectionInfo,":");
        try{
            host=st.nextToken();
            port=new Integer(st.nextToken()).intValue();
        }catch(java.util.NoSuchElementException e){
            System.err.println("You did something wrong with the formatting, I'm switching back to the defaults");
            host="localhost";
            port=5001;
        }catch(java.lang.NullPointerException e){
            System.err.println("You did something wrong with the formatting, I'm switching back to the defaults");
            host="localhost";
            port=5001;
        }
        initVariables();
    }





    /*Name:initVariables
     *Returns:  None
     *Arguments: none
     *Description: A private function used to initialize the
                    return data structure and the buffers
     *Modifications: None
     *Version: 1.1
     */
    private void initVariables(){
        retStringBuffer=new StringBuffer();
        // list of commands that return data
        // decided to hardcode here because didn't want to worry about
        // permissionto read the file this is stored in (in case this class
        // is used in a applet) and location of the file (URL or
        // absolute/relative path)
        // NOTE:  This list will have to be updated to include new commands.
        //        <CommandName>  <MultipleMessageData?  (0,1)>

        returnDataHash = new Hashtable<String, String>();

		// *****Keep this list in Alphabetical order!*****
		//  Update files: connect.dat and StkCon.java to match list...

		// Begin ReturnsDataList
        returnDataHash.put("3DGETVIEWPOINT",         "0");
        returnDataHash.put("ACATEVENTS_RM",			 "1"); // Multiple
        returnDataHash.put("ACATPROBABILITY_R",      "0");
        returnDataHash.put("ACCESS",                 "0");
        returnDataHash.put("ACCESSINFO_R",           "0");
		returnDataHash.put("ACCESSPARAMETERS_RM",    "1"); // Multiple
        returnDataHash.put("AER",                    "0");
        returnDataHash.put("ALLACCESS",              "1"); // Multiple
        returnDataHash.put("ALLINSTANCENAMES",       "0");
        returnDataHash.put("ANIMFRAMERATE",          "0");
        returnDataHash.put("ANTENNA_RM",             "1"); // Multiple
	    returnDataHash.put("ASTROGATOR_RM",          "1"); // Multiple
		returnDataHash.put("ASYNCALLOWED_R",         "0");
		returnDataHash.put("ATTCOV_RM",              "1"); // Multiple
		returnDataHash.put("ATMOSPHERE_RM",          "1"); // Multiple
		returnDataHash.put("AUTHOR_RM",              "1"); // Multiple
        returnDataHash.put("AVIATOR_RM",             "1"); // Multiple
		returnDataHash.put("CALCULATIONTOOL_R",      "0");
		returnDataHash.put("CALCULATIONTOOL_RM",     "1"); // Multiple
        returnDataHash.put("CAT_RM",      		     "1"); // Multiple
		returnDataHash.put("CENTRALBODY",            "0");
		returnDataHash.put("CENTRALBODY_R",          "0");
		returnDataHash.put("CENTRALBODYNAMES",       "0");
		returnDataHash.put("CHAINS_R",      	     "0");
        returnDataHash.put("CHAINS_RM",      	     "1"); // Multiple
        returnDataHash.put("CHAINALLACCESS",         "1"); // Multiple
        returnDataHash.put("CHAINGETACCESSES",       "1"); // Multiple
        returnDataHash.put("CHAINGETINTERVALS",      "1"); // Multiple
        returnDataHash.put("CHAINGETSTRANDS",        "1"); // Multiple
        returnDataHash.put("CHECKISAPPBUSY",         "0");
        returnDataHash.put("CHECKSCENARIO",          "0");
        returnDataHash.put("CLOSEAPPROACH",          "1"); // Multiple
		returnDataHash.put("COLLECTION_RM",          "1"); // Multiple
        returnDataHash.put("COMMQUERY",              "0");
        returnDataHash.put("COMMSYSTEM_RM",          "1");
		returnDataHash.put("COMPONENTBROWSER_RM",    "1"); // Multiple
		returnDataHash.put("COMPUTECRDN",            "0");
		returnDataHash.put("CONEXPORTCONFIG_R",      "0");
		returnDataHash.put("CONSTELLATION_R",        "0");
		returnDataHash.put("CONVERT",                "0");
		returnDataHash.put("CONVERTCOORD",           "0");
        returnDataHash.put("CONVERTDATE",            "0");
		returnDataHash.put("CONVERTUNIT",            "0");
		returnDataHash.put("COV_R",      	 	     "0");
        returnDataHash.put("COV_RM",      	 	     "1"); // Multiple
        returnDataHash.put("DECKACCESS",             "1"); // Multiple
		returnDataHash.put("DISPERSIONELLIPSE_R",    "0");
        returnDataHash.put("DISQUERY",               "0");
        returnDataHash.put("DOESOBJEXIST",           "0");
        returnDataHash.put("ENVIRONMENT_RM",         "1"); // Multiple
        returnDataHash.put("EOIR_R",                 "0");
        returnDataHash.put("EXPORTCONFIG_R",         "0");
        returnDataHash.put("FIELDOFVIEW_RM",         "1"); // Multiple
        returnDataHash.put("GETACCESSES",            "1"); // Multiple
        returnDataHash.put("GETANIMTIME",            "0");
        returnDataHash.put("GETANIMATIONDATA",       "0");
        returnDataHash.put("GETATTITUDE",            "1"); // Multiple
		returnDataHash.put("GETATTITUDETARG",        "0");
		returnDataHash.put("GETBOUNDARY",            "1"); // Multiple
        returnDataHash.put("GETCONVERSION",          "0");
        returnDataHash.put("GETDB",                  "0");
        returnDataHash.put("GETDEFAULTDIR",          "0");
        returnDataHash.put("GETDESCRIPTION",         "0");
        returnDataHash.put("GETDIRECTORY",         	 "0");
        returnDataHash.put("GETDSPFLAG",             "0");
        returnDataHash.put("GETDSPINTERVALS",        "0");
		returnDataHash.put("GETDSPTIMES",            "0");
        returnDataHash.put("GETEPOCH",               "0");
		returnDataHash.put("GETFULLREPORT",          "1"); // Multiple
		returnDataHash.put("GETIPCVERSION",          "0");
		returnDataHash.put("GETLASTCOMMAND",         "1"); // Multiple
		returnDataHash.put("GETLICENSES",            "1"); // Multiple
		returnDataHash.put("GETLINE",                "1"); // Multiple
		returnDataHash.put("GETMAPSTYLES_R",         "0");
		returnDataHash.put("GETMARKERLIST",          "1"); // Multiple
		returnDataHash.put("GETMESSAGELOGFILE",      "0");
		returnDataHash.put("GETNUMNOTES",            "0");
		returnDataHash.put("GETPROPNAME",            "1"); // Multiple
        returnDataHash.put("GETPROPERTIES",          "0");
        returnDataHash.put("GETREPORT",              "1"); // Multiple
        returnDataHash.put("GETRPTSUMMARY",          "1"); // Multiple
        returnDataHash.put("GETSCENPATH",            "0");
        returnDataHash.put("GETSTKHOMEDIR",          "0");
        returnDataHash.put("GETSTKVERSION",          "0");
        returnDataHash.put("GETTIMEPERIOD",          "0");
        returnDataHash.put("GETLOCALTIMEZONENAME_R", "0");
        returnDataHash.put("GETUSERDIR",             "0");
        returnDataHash.put("GRAPHICS_R",      	     "0");
        returnDataHash.put("GRIDINSPECTOR",          "0");
		returnDataHash.put("GROUNDELLIPSE_R",        "0");
		returnDataHash.put("KEYVALUEMETADATA_RM",    "1"); // Multiple
        returnDataHash.put("LASERENVIRONMENT_RM",    "1"); // Multiple
        returnDataHash.put("LICENSE_RM",      	     "1"); // Multiple
        returnDataHash.put("LIFETIME",               "0");
        returnDataHash.put("LIGHTINGGRAZINGTYPE_R",  "0");
        returnDataHash.put("LIGHTINGMAXSTEP_R",      "0");
		returnDataHash.put("LISTOPERATOR",           "0");
        returnDataHash.put("LISTSUBOBJECTS",         "0");
		returnDataHash.put("MAPANNOTATION_RM",       "1"); // Multiple
        returnDataHash.put("MAPID_R",      	 	     "0");
        returnDataHash.put("MATLAB_R",      	     "0");
		returnDataHash.put("MEASURESURFACEDISTANCE", "0");
		returnDataHash.put("MINERVA_RM",             "1"); // Multiple
		returnDataHash.put("MISSIONMODELER_RM",      "1"); // Multiple
		returnDataHash.put("ONEPOINTACCESS",         "1"); // Multiple
		returnDataHash.put("PARALLEL_RM",            "1"); // Multiple
		returnDataHash.put("PERCENTCOMPLETE_R",      "0");
		returnDataHash.put("POSITION",               "0");
		returnDataHash.put("POSITION_RM",            "1"); // Multiple
        returnDataHash.put("QUICKREPORT_RM",         "1"); // Multiple
        returnDataHash.put("RADAR_RM",               "1"); // Multiple
        returnDataHash.put("RADARCLUTTER_RM",        "1"); // Multiple
        returnDataHash.put("RANGE_RM",        		 "1"); // Multiple
		returnDataHash.put("RCS_R",                  "0");
		returnDataHash.put("RCS_RM",                 "1"); // Multiple
        returnDataHash.put("RECEIVER_RM",            "1"); // Multiple
		returnDataHash.put("RECORDMOVIE2D_R",        "0");
		returnDataHash.put("REPORT_RM",              "1"); // Multiple
		returnDataHash.put("RFCHANNELMODELER_RM",    "1"); // Multiple
		returnDataHash.put("RFCHANNELMODELERGRAPHICS_RM",    "1"); // Multiple
        returnDataHash.put("RFENVIRONMENT_RM",       "1"); // Multiple
		returnDataHash.put("SCHED",                  "1"); // Multiple
		returnDataHash.put("SEDS_RM",                "1"); // Multiple
        returnDataHash.put("SENSORQUERY",            "0");
		returnDataHash.put("SHOWNAMES",              "0");
        returnDataHash.put("SHOWUNITS",              "0");
        returnDataHash.put("SOC_RM",                 "1"); // Multiple
        returnDataHash.put("SOFTVTR2D_R",            "0");
        returnDataHash.put("SNTCONFIGIMPORTER_RM",   "1"); // Multiple
        returnDataHash.put("SNTSCENARIOEXPLORER_RM", "1"); // Multiple
		returnDataHash.put("SPATIALTOOL_R",          "0");
		returnDataHash.put("SPATIALTOOL_RM",         "1"); // Multiple
		returnDataHash.put("SPICE_RM",               "1"); // Multiple
		returnDataHash.put("STARDATA_RM",            "1"); // Multiple
        returnDataHash.put("STOPWATCHGET",           "0");
		returnDataHash.put("TERRAIN_RM",             "1"); // Multiple
		returnDataHash.put("TERRAINSERVER_RM",       "1"); // Multiple
		returnDataHash.put("TE_TRACKCOMPARISONCALCULATOR_RM", "1"); // Multiple
		returnDataHash.put("TE_TRACKTRACEABILITY_RM", "1"); // Multiple
		returnDataHash.put("TE_TRACKTRUTHMATCH_RM",  "1"); // Multiple
		returnDataHash.put("TIMETOOL_R",      	     "0");
		returnDataHash.put("TIMETOOL_RM",      	     "1"); // Multiple
        returnDataHash.put("TRANSMITTER_RM",         "1"); // Multiple
		returnDataHash.put("UNITS_GET",         	 "0");
		returnDataHash.put("UNITS_CONVERT",      	 "0");
		returnDataHash.put("VECTORTOOL_R",      	 "0");
		returnDataHash.put("VECTORTOOL_RM",      	 "1"); // Multiple
		returnDataHash.put("VIEWER_RM",              "1"); // Multiple
		returnDataHash.put("VISIBILITY_RM",      	 "1"); // Multiple
		returnDataHash.put("VO_R",      	 	     "0");
		returnDataHash.put("VO_RM",                  "1"); // Multiple
		returnDataHash.put("VOLUMEGEOMETRY_R",       "0");
		returnDataHash.put("VOLUMEGEOMETRY_RM",      "1"); // Multiple
		returnDataHash.put("VOLUMETRIC_RM",          "1"); // Multiple
		returnDataHash.put("WINDOW2D_R",      	     "0");
		returnDataHash.put("WINDOW3D_R",      	     "0");
        returnDataHash.put("ZOOM_R",      	 	     "0");
		// End ReturnsDataList

        returnedAck=false;
    }





    /*Name:connect
     *Returns:  None
     *Arguments: none
     *Description: Connects to STK with the given host/port data
                    Returns 0 if a socket is opened or successfully created
                    Returns -1 if there was an error
     *Modifications: 1.1    Added ConControl command to correctly setup the
                            Socket for better report grabbing from STK
                     1.2    Changed default status to AckOn instead of AckOff
     *Version: 1.0,1.1,1.2
     */
    public int connect() throws IOException, NumberFormatException {
        if (socket == null)
        {
            // open socket, get output/input streams for socket
            try
            {
                socket = new Socket(host, port);
				socket.setTcpNoDelay(true);
                toStk = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                fromStk = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }
            catch (Exception e)
            {
                socket = null;
                toStk = null;
                fromStk = null;
                System.out.println("Error connecting to " + host + ":" + port + " -> "  + e);
                return -1;
            }

			ackOn();
			asyncOff();
            sendConCommand("Concontrol", "/", "ErrorOn VerboseOff");
            return 0;
        }
        else
        {
            System.out.println("Already connected to " + host + ":" + port);
            return 0;
        }
    }




    /*Name:disconnect
     *Returns:  None
     *Arguments: None
     *Description: Disconnects from STK
     *Modifications: None
     *Version: 1.0
     */
    public void disconnect() {
        if (socket != null)
        {
            toStk.println("Concontrol / disconnect");
            toStk.flush();
            try
            {
                toStk.close();
                fromStk.close();
                socket.close();
            }
            catch (IOException ioe) {}
            socket = null;
            toStk = null;
            fromStk = null;
        }
    }




    /*Name:sendConCommand
     *Returns:  String
     *Arguments: String command, String objectPath, string Data
     *Description: Sends a connect command distributed among the three pieces.
                    The return is the return data from the subsequent command.
                    the command is the first word, such as "New", the object
                    path is the STK object path, such as "Scenario/Scen1/Satellite/Sat1"
                    and the data is the entire rest of the string
     *Modifications: 1.1    -Revamped the return string storage to use a
                            StringBuffer instead of a string.  This significantly
                            reduces overhead of returning reports.
                     1.2    -Rewrote the part of the code which checked the status of
                            ACK.  This will now be able to read "ConControl" statements
                            with multiple arguments.
                            -Rewrote the return code so that if Ack is turned on, the system
                            won't wait for returned data when an error on the STK side
                            occurs.
                            -The ACK status is no longer written in the returned string,
                            but instead is stored in a class-wide variable "returnedAck"
                            which is readable with the "getAckStatus" method

     *Version: 1.0,1.1,1.2
     */
    public String sendConCommand(String cmd, String objectPath, String cmdData) throws IOException, NumberFormatException
    {
        return sendConCommand(cmd + " " + objectPath + " " + cmdData);
    }





    /*Name:sendConCommand
     *Returns:  String
     *Arguments: String command
     *Description: Same method as the other sendConCommand, but the entire
                    command is stored in one  string.
     *Modifications:1.2    -Rewrote the part of the code which checked the status of
                            ACK.  This will now be able to read "ConControl" statements
                            with multiple arguments.
                            -Rewrote the return code so that if Ack is turned on, the system
                            won't wait for returned data when an error on the STK side
                            occurs.
                            -The ACK status is no longer written in the returned string,
                            but instead is stored in a class-wide variable "returnedAck"
                            which is readable with the "getAckStatus" method
     *Version: 1.1,1.2
     */
    public String sendConCommand(String inputCommand) throws IOException, NumberFormatException
    {
        StringTokenizer st;
        boolean blankLine;
        String cmd,cmdData,buffer,command;
        boolean foundAck,foundAsync;

        retStringBuffer.setLength(0);
        if ((socket != null) && (toStk != null) && (fromStk != null)){
            st=new StringTokenizer(inputCommand);

            if(st.hasMoreTokens()){
                cmd=st.nextToken();
                blankLine=false;
            }
            else{
                cmd=" ";
                blankLine=true;
            }
            cmdData="";

            if(cmd.equals("CONCONTROL")){
                foundAck = false;
                foundAsync = false;
                while(st.hasMoreTokens()){
                    buffer=st.nextToken();
                    if(buffer!=null){
                        cmdData+=buffer+" ";
                        if(!foundAck){
                            if(buffer.equalsIgnoreCase("ACKON")){
                                ack = true;
                                foundAck = true;
                            }
                            else if(buffer.equalsIgnoreCase("ACKOFF")){
                                ack = false;
                                foundAck = true;
                            }
                        }
                        if(!foundAsync){
                            if(buffer.equalsIgnoreCase("ASYNCON")){
                                async = true;
                                foundAsync = true;
                            }
                            else if(buffer.equalsIgnoreCase("ASYNCOFF")){
                                async = false;
                                foundAsync = true;
                            }
                        }
                    }
                }
            }
            else{
                while(st.hasMoreTokens()){
                    cmdData+=st.nextToken()+" ";
                }
            }
            // send command to stk
            if(!blankLine){
				if(cmd.equalsIgnoreCase("SetState") && cmdData.contains("TLE"))
					command = inputCommand;
            	else
                	command = cmd+" "+cmdData;
                toStk.println(command);
                toStk.flush();

                // read ack/nack message
                if (ack){
                    buffer=readAck();
                    if(buffer!=null){
                        if(buffer.equalsIgnoreCase("ACK  ")){
                            returnedAck=true;
                        }
                        else{
                            returnedAck=false;
                        }
                    }
                    else{
                        returnedAck=false; //want to short circuit if nothing back
                    }
                }
                else{
                    returnedAck=false;
                }

                // read data from stk if command returns data
                // in the case of Ack, must have returned ACK as well
                String CommandUpcase = cmd.toUpperCase();
                if (returnDataHash.containsKey(CommandUpcase)){
                    if ((ack&&returnedAck) ||
                        (!ack)){
                        int multi = 0;

                        multi = convertString2Int((String)returnDataHash.get(CommandUpcase));

                        int hdVal = read40ByteHeader();

                        // single message format
                        if (multi == 0)
                        {
                           // retString = retString + readNBytes(hdVal);
                           retStringBuffer.append(readNBytes(hdVal));
                        }
                        else  // multiple message format
                        {
                            // get the number of "records" to be returned
                            String multiHeader = readNBytes(hdVal);
                            multiHeader = multiHeader.replace('\n', ' ');

                            StringTokenizer multiTok = new StringTokenizer(multiHeader, " ");
                            int  numRecs = 0;
                            numRecs = convertString2Int(multiTok.nextToken());

                            // loop and read the "records"
                            int numBytes = 0;
                            for (int i=0; i<numRecs; i++)
                            {
                                // read in single message header
                                numBytes = read40ByteHeader();

                               // retString = retString + readNBytes(numBytes);
                               retStringBuffer.append(readNBytes(numBytes));
                               retStringBuffer.append('\n');
                            }
                        }
                    }
                }
            }

            return retStringBuffer.toString();
        }
        else
        {
            return null;
        }
    }

    /*Name:readAck
     *Returns:  String
     *Arguments: None
     *Description: Returns whether STK returned an ACK or a NACK
     *Modifications: None
     *Version: 1.0
     */
    private String readAck() throws IOException {
        String sReturnString;
		String sAck;
		sAck = readNBytes (3);
        if (sAck.charAt(0) == 'A')
        {
            sReturnString = new String("ACK  ");
        }
        else
        {
			readNBytes (1);
			sReturnString = new String("NACK ");
        }

        return sReturnString;
    }





    /*Name:read40ByteHeader
     *Returns:  int
     *Arguments: None
     *Description: reads the 40 byte header.
                    returns the integer value after the command.
     *Modifications: None
     *Version: 1.0
     */
    private int read40ByteHeader() throws IOException, NumberFormatException {
		String sRawHeader;
		sRawHeader = readNBytes (40);
        String sHeader = sRawHeader.replace('\n', ' ');
        StringTokenizer stok = new StringTokenizer(sHeader, " ");
        stok.nextToken();
        int headerVal = 0;
        try
        {
            headerVal = Integer.parseInt(stok.nextToken().trim());
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("Error parsing header info: " + nfe);
			throw nfe;
        }
        return headerVal;
    }





    /*Name:readNBytes
     *Returns:  int
     *Arguments: None
     *Description: reads N bytes from the socket and returns a string
     *Modifications: None
     *Version: 1.0
     */
    private String readNBytes(int numBytes) throws IOException {
        char[] czBuffer = new char[numBytes];

        try
        {
			int iCurrentBytesRead,
				iBytesToRead = numBytes,
				iBufferLocation = 0,
				iTotalBytesRead = 0;

			while(iTotalBytesRead < numBytes)
			{
				iCurrentBytesRead = fromStk.read(	czBuffer,
													iBufferLocation,
													iBytesToRead );
				iTotalBytesRead += iCurrentBytesRead;
				iBufferLocation += iCurrentBytesRead;
				iBytesToRead    -= iCurrentBytesRead;
			}
        }
        catch (IOException ioe)
        {
            System.out.println("Error reading from Stk socket:  " + ioe);
			throw ioe;
        }

        return (new String(czBuffer));
    }





    /*Name:getAckStatus
     *Returns:  boolean
     *Arguments: None
     *Description: returns true if the last command successeded
                   returns false if the last command failed, or
                   Ack is turned off entirely
     *Modifications: None
     *Version: 1.2
     */
    public boolean getAckStatus(){
        return(returnedAck);
    }


    /*Name:AckOn
     *Returns:  None
     *Arguments: None
     *Description: sets Ack to on
     *Modifications: None
     *Version: 1.3
     */
    public void ackOn() throws IOException {
		ack = true;
		sendConCommand("ConControl / AckOn");
	}

    /*Name:AckOff
     *Returns:  None
     *Arguments: None
     *Description: sets Ack to off
     *Modifications: None
     *Version: 1.3
     */
    public void ackOff() throws IOException {
		ack = false;
		sendConCommand("ConControl / AckOff");
	}

    /*Name:isAckOn
     *Returns:  None
     *Arguments: None
     *Description: returns whether ack is on
     *Modifications: None
     *Version: 1.3
     */
    public boolean isAckOn(){
		return ack;
	}

    /*Name:asyncOn
     *Returns:  None
     *Arguments: None
     *Description: sets async to on
     *Modifications: None
     *Version: 1.3
     */
    public void asyncOn() throws IOException {
		async = true;
		sendConCommand("ConControl / AsyncOn");
	}

    /*Name:asyncOff
     *Returns:  None
     *Arguments: None
     *Description: sets async to off
     *Modifications: None
     *Version: 1.3
     */
    public void asyncOff() throws IOException {
		async = false;
		sendConCommand("ConControl / AsyncOff");
	}

    /*Name:isAsyncOn
     *Returns:  None
     *Arguments: None
     *Description: returns whether async is on
     *Modifications: None
     *Version: 1.3
     */
    public boolean isAsyncOn(){
		return async;
	}


    /*Name:readAsync
     *Returns:  String Array
     *Arguments: None
     *Description: reads return string array from async commands.
     *Modifications: None
     *Version: 1.3
     */
	public String[] readAsync() throws IOException {
        int NumOfPackets;
        int NumValues;
        String outputdata = "";
        String[] AsyncHeader;
        String[] outputdataarray;
        StringTokenizer st;

        AsyncHeader = readAsyncHeader();
        NumOfPackets = convertString2Int(AsyncHeader[7]);
        for(int i = 1; i <= NumOfPackets; i++){
            outputdata = readNBytes(convertString2Int(AsyncHeader[9]));
            if (i < convertString2Int(AsyncHeader[8])) {
                AsyncHeader = readAsyncHeader();
            }
        }

        st=new StringTokenizer(outputdata,"\n");
        NumValues = st.countTokens();
        outputdataarray = new String[NumValues];
        for(int i = 0; i < NumValues; i++){
            outputdataarray[i] = st.nextToken();
		}
        return outputdataarray;
    }

    /*Name:readAsyncHeader
     *Returns:  String Array
     *Arguments: None
     *Description: reads async message header.
     *Modifications: None
     *Version: 1.3
     */
    private String[] readAsyncHeader() throws IOException {
		String syncPattern, headerLength, headerVer, headerRev, typeLength, asyncType,
		    ident, totalPack, packNum, dataLength;
        String[] outputheaderarray = new String[10];
        syncPattern = readNBytes(3);
        headerLength = readNBytes(2);
        headerVer = readNBytes(1);
        headerRev = readNBytes(1);
        typeLength = readNBytes(2);
        asyncType = readNBytes(15);
        ident = readNBytes(6);
        totalPack = readNBytes(4);
        packNum = readNBytes(4);
        dataLength = readNBytes(4);

        outputheaderarray[0] = syncPattern;
        outputheaderarray[1] = headerLength;
        outputheaderarray[2] = headerVer;
        outputheaderarray[3] = headerRev;
        outputheaderarray[4] = typeLength;
        outputheaderarray[5] = asyncType;
        outputheaderarray[6] = ident;
        outputheaderarray[7] = totalPack;
        outputheaderarray[8] = packNum;
        outputheaderarray[9] = dataLength;
//for(int i=0;i<10;i++){
//  System.out.println(i+": "+outputheaderarray[i]);
//}
        return outputheaderarray;

    }

    /*Name:convertString2Int
     *Returns:  int
     *Arguments: String
     *Description: converts string to integer.
     *             If not a valid int, then returns zero.
     *Modifications: None
     *Version: 1.3
     */
    private int convertString2Int(String num) throws NumberFormatException{
       int z;
       try{
           z = Integer.parseInt(num);
	   }catch (NumberFormatException nfe){
           z = 0;
           throw nfe;
       }
       return z;
    }
}
