package com.foreks.backbone.trapper;


import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class TrapSender {
    private final String community;
    private final String trapOid;
    private final String ipAddress;
    private final int    port;

    public TrapSender(final String ipAdress, final int port, final String community, final String trapOid) {
        this.ipAddress = ipAdress;
        this.port = port;
        this.community = community;
        this.trapOid = trapOid;
    }

    /**
     * This methods sends the V1 trap
     */
    public void sendSnmpV1Trap() {
        try {
            // Create Transport Mapping
            final TransportMapping transport = new DefaultUdpTransportMapping();
            transport.listen();

            // Create Target
            final CommunityTarget comtarget = new CommunityTarget();
            comtarget.setCommunity(new OctetString(this.community));
            comtarget.setVersion(SnmpConstants.version1);
            comtarget.setAddress(new UdpAddress(this.ipAddress + "/" + this.port));
            comtarget.setRetries(2);
            comtarget.setTimeout(5000);

            // Create PDU for V1
            final PDUv1 pdu = new PDUv1();
            pdu.setType(PDU.V1TRAP);
            pdu.setEnterprise(new OID(this.trapOid));
            pdu.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
            pdu.setSpecificTrap(1);
            pdu.setAgentAddress(new IpAddress(this.ipAddress));
            final long sysUpTime = 111111;
            pdu.setTimestamp(sysUpTime);
            pdu.add(new VariableBinding(new OID(this.trapOid), new Integer32(123456)));
            // Send the PDU
            final Snmp snmp = new Snmp(transport);
            System.out.println("Sending V1 Trap to " + this.ipAddress + " on Port " + this.port);
            snmp.send(pdu, comtarget);
            snmp.close();
        } catch (final Exception e) {
            System.err.println("Error in Sending V1 Trap to " + this.ipAddress + " on Port " + this.port);
            System.err.println("Exception Message = " + e.getMessage());
        }
    }

    /**
     * This methods sends the V2 trap to the Localhost in port 163
     */
    public void sendSnmpV2Trap() {
        try {
            // Create Transport Mapping
            final TransportMapping transport = new DefaultUdpTransportMapping();
            transport.listen();

            // Create Target
            final CommunityTarget comtarget = new CommunityTarget();
            comtarget.setCommunity(new OctetString(this.community));
            comtarget.setVersion(SnmpConstants.version2c);
            comtarget.setAddress(new UdpAddress(this.ipAddress + "/" + this.port));
            comtarget.setRetries(2);
            comtarget.setTimeout(5000);

            // Create PDU for V2
            final PDU pdu = new PDU();

            // need to specify the system up time
            final long sysUpTime = 111111;
            pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(sysUpTime)));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(this.trapOid)));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(this.ipAddress)));

            // variable binding for Enterprise Specific objects, Severity (should be defined in MIB file)
            pdu.add(new VariableBinding(new OID(this.trapOid), new OctetString("Major")));
            pdu.setType(PDU.NOTIFICATION);

            // Send the PDU
            final Snmp snmp = new Snmp(transport);
            System.out.println("Sending V2 Trap to " + this.ipAddress + " on Port " + this.port);
            snmp.send(pdu, comtarget);
            snmp.close();
        } catch (final Exception e) {
            System.err.println("Error in Sending V2 Trap to " + this.ipAddress + " on Port " + this.port);
            System.err.println("Exception Message = " + e.getMessage());
        }
    }
}