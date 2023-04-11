package Machine;
import java.io.*;
import java.util.*;
import java.net.*;
import Message.*;

public class Machine {
    String ac_address, clientIP;
    int ac_port;
    Vector < Packet > buffer;
    Vector < Packet > receiveBuffer;

    public Machine(String ac_address, int ac_port, String clientIP) throws IOException {
        this.ac_address = ac_address;
        this.clientIP = clientIP;
        this.ac_port = ac_port;
        this.buffer = new Vector < Packet > ();
        this.receiveBuffer = new Vector < Packet > ();
    }

    public void initiate() throws IOException {
        Scanner sc = new Scanner(System.in);
        try {
        	
            Socket s = new Socket(ac_address, ac_port);
            
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
            System.out.printf("Connected to server %s:%d\n", ac_address, ac_port);

            String certID;
            SecurityCertificate cert = new SecurityCertificate();

            System.out.printf("Enter username: ");
//            cert.username = sc.next();
            cert.username = "marvy";

            System.out.printf("Enter password: ");
//            cert.password = sc.next();
            cert.password = "admin";
            
            oos.writeObject(cert);

            while (true) {
                try {
                    cert = (SecurityCertificate) ois.readObject();
                    if (cert != null) break;
                } catch (IOException e) {

                } catch (ClassNotFoundException e) {

                }
            }

            if (cert.CertificateID.equals("NULL")) {
                System.out.println("Wrong Credentials provided!!");
                return;
            }

            certID = cert.CertificateID;
            System.out.printf("Security ID: %sn\n", certID);

            System.out.printf("1.Get file\n2.Idle state\n\n>>");
            int x = sc.nextInt();
            

            if (x == 1) {
                Packet first = new Packet(0);
                String path, destIP;
                int windowSize = 0;

                System.out.printf("Name file to fetch: ");
//                path = sc.next();
                path = "file.txt";

                System.out.printf("IP of Destination: ");
//                destIP = sc.next();
                destIP = "192.168.1.4";
                
                System.out.printf("Window Size: ");
//                windowSize = sc.nextInt();
                windowSize = 20;
                

                first.cert_id = certID;
                first.client_ip = clientIP;
                first.destination_ip = destIP;
                first.pkt_id = -2;
                first.pkt_no = windowSize;
                first.msg_name = path;

                oos.writeObject(first);

                System.out.printf("Waiting for file..");

                Packet p;
                int totalPkts = 0;
                int byteLength = 0;
                
                Packet firstReply = (Packet) ois.readObject();
                totalPkts = Integer.parseInt(firstReply.msg_name);
                byteLength = firstReply.pkt_no;
                System.out.println("Ready to receive " + totalPkts + " from " + firstReply.client_ip);
                

                while (true) {
                    try {
                        p = (Packet) ois.readObject();
                        if (p != null) {
                            if (!p.cert_id.equals(certID)) {
                                System.out.println("SECURITY CERTIFICATE MISMATCH!\n");
                                return;
                            }
                            else {
                                int cnt = Math.round(p.pkt_id * 20 / totalPkts);
                                String cur = "|";
                                for (int i = 0; i < 20; i++) {
                                    if (i < cnt) {
                                        cur += "=";
                                    } else if (i == cnt) {
                                        cur += ">";
                                    } else {
                                        cur += " ";
                                    }
                                }
                                cur += "|" + p.pkt_id + "/" + totalPkts + "\r";
//                                System.out.printf(cur);
                        		System.out.printf("%d recieved\n", p.pkt_id);
                                receiveBuffer.add(p);
                                
                                if (receiveBuffer.size() % windowSize == 0 || p.pkt_id == totalPkts) {
                                	Random random = new Random();
                                	int rand = random.nextInt()%2;
                                	if (rand == 0) {
	                                	Packet ack = new Packet(0);
	                                	ack.msg_name = "ack";
	                                	ack.pkt_id = receiveBuffer.size();
	                                	ack.cert_id = certID;
	                                	ack.destination_ip = destIP;
	                                    ack.client_ip = clientIP;
	                                	oos.writeObject(ack);
	                                	System.out.println("Ack sent!");
                                	}
                                	else {
                                		System.out.println("Ack not sent, waiting for prev window again.");
                                		for (int i=receiveBuffer.size()-1; i>=0; i--) {
                                			if (receiveBuffer.get(i).pkt_id % windowSize != 0) {
                                				receiveBuffer.remove(i);
                                			}
                                			else break;
                                		}
                                	}
                                }

                                if (p.pkt_id == totalPkts) {
                                    System.out.printf("Received %d packets from %s\n", totalPkts, p.client_ip);
                                    break;
                                }
                            }
                        }

                    } catch (ClassNotFoundException e) {
                        System.out.printf("Error reading packets (Undefined Format): ");
                        e.printStackTrace();
                        try {
                            oos.close();
                            ois.close();
                            s.close();
                        } catch (IOException e1) {
                            System.out.printf("Error closing connection: ");
                            e1.printStackTrace();
                        }
                    } catch (IOException e) {
                        System.out.printf("Error receiving packets!!");
                        e.printStackTrace();
                        try {
                            oos.close();
                            ois.close();
                            s.close();
                        } catch (IOException e1) {
                            System.out.printf("Error closing connection: ");
                            e1.printStackTrace();
                        }

                    }
                }

                final String outputPath = receiveBuffer.get(0).msg_name;

                System.out.printf("Received " + outputPath + " from " + receiveBuffer.get(0).client_ip + "\n");

                byte[] byteFile = new byte[byteLength];
                
                int i=0;
                for (Packet pkt: receiveBuffer) {
//                    Packet pkt = receiveBuffer.get(i);
                    for (int j = 0; j < pkt.payload.length && i < byteLength; j++) {
                        byteFile[i++] = pkt.payload[j];
                    }
                }

                try {
                    FileOutputStream fos = new FileOutputStream(outputPath);
                    fos.write(byteFile);
                    fos.close();
                    System.out.printf("File Saved to disk!\n");
                } catch (IOException e) {
                    System.out.println("Error Saving file to disk!");
                }



            } else if (x == 2) {

                Packet first = (Packet) ois.readObject();
                String path = first.msg_name;
                String destIP = first.client_ip;
                int windowSize = first.pkt_no;

                byte[] file;
                File fileobj = new File(path);

                try {
                    FileInputStream fis = new FileInputStream(fileobj);
                    file = new byte[(int) fileobj.length()];
                    fis.read(file);
                    fis.close();
                } catch (IOException e) {
                    System.out.print("Exception while opening file: ");
                    e.printStackTrace();
                    ois.close();
                    oos.close();
                    s.close();
                    return;
                }

                int pyld_size = 7;
                final int pkt_total = file.length / pyld_size + (file.length % pyld_size == 0 ? 0 : 1);

                System.out.printf("\nMessage Size: %d Payload Size: %d Total Packets: %d\n\n", file.length, pyld_size, pkt_total);

                int index = 0;
                Packet initPkt = new Packet(0);
                initPkt.destination_ip = destIP;
                initPkt.client_ip = clientIP;
                initPkt.msg_name = Integer.toString(pkt_total);
                initPkt.pkt_id = -1;
                initPkt.pkt_no = file.length;
                initPkt.cert_id = certID;
                oos.writeObject(initPkt);
                System.out.printf("Sending %d packets to %s\n", pkt_total, initPkt.destination_ip);

                for (int i = 0; i < pkt_total; i++) {
                    Packet pkt = new Packet(pyld_size);
                    pkt.client_name = "localhost";
                    pkt.client_ip = clientIP;
                    pkt.destination_ip = destIP;
                    pkt.pkt_no = i + 1;
                    pkt.pkt_id = i + 1;
                    pkt.msg_name = path;
                    pkt.cert_id = certID;
                    int j = 0;
                    for (; j < pyld_size && index < file.length; j++) {
                        pkt.payload[j] = (byte) file[index];
                        index++;
                    }

                    buffer.add(pkt);
                }
                
                for (int j=0; j<buffer.size(); j++) {
                	try {
	                	
	                	Packet p = buffer.get(j);
	                	oos.writeObject(p);
	                	
	                	int cnt = Math.round(p.pkt_id * 20 / pkt_total);
	                    String cur = "|";
	                    for (int i = 0; i < 20; i++) {
	                        if (i < cnt) {
	                            cur += "=";
	                        } else if (i == cnt) {
	                            cur += ">";
	                        } else {
	                            cur += " ";
	                        }
	                    }
	                    cur += "|" + p.pkt_id + "/" + pkt_total + "\r";
//	                    System.out.printf(cur);
	                    System.out.printf("%d sent\n", p.pkt_id);
	                    if ((j+1)%windowSize == 0) {
	                    	try {
		                    	System.out.printf("Waiting for ack");
		                    	s.setSoTimeout(5000);
	                    		Packet ack = (Packet) ois.readObject();
	                    		System.out.println("\nAck Recieved!");
	                    		
	                    	}
	                		catch(SocketException e) {
	                			s.setSoTimeout(Integer.MAX_VALUE);
	                			System.out.printf("Resending previous window\n");
	                			j -= windowSize;
	                		}
	                    	finally {
	                    		s.setSoTimeout(Integer.MAX_VALUE);
	                    	}
	                		
	                	}
	                    
	                    if (p.pkt_id == pkt_total) {
	                        System.out.printf("Sent %d packets to %s\n", pkt_total, p.destination_ip);
	                        break;
	                    }
                	}catch (IOException e) {
                        System.out.printf("Error sending packets: ");
                        e.printStackTrace();

                        try {
                            s.close();
                            ois.close();
                            oos.close();
                        } catch (IOException e1) {
                            System.out.printf("Error closing connection: ");
                            e1.printStackTrace();
                        }
                    }
                }
                
            }

        } catch (Exception e) {
            System.out.printf("There was an error connecting to the server: ");
            e.printStackTrace();
        }
    }
}