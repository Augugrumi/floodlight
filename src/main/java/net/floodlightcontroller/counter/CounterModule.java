package net.floodlightcontroller.counter;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.OFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.*;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class CounterModule implements IOFMessageListener, IFloodlightModule {

    private IFloodlightProviderService floodlightProvider;
    private Logger logger;
    private AtomicLong counter;

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

        switch (msg.getType()) {
            case PACKET_IN:
                Ethernet l2 = new Ethernet();
                l2.setSourceMACAddress(MacAddress.of("00:00:00:00:00:01"));
                l2.setDestinationMACAddress(MacAddress.BROADCAST);
                l2.setEtherType(EthType.IPv4);

                IPv4 l3 = new IPv4();
                l3.setSourceAddress(IPv4Address.of("10.0.0.0"));
                l3.setDestinationAddress(IPv4Address.of("10.0.0.3"));
                l3.setTtl((byte) 64);
                l3.setProtocol(IpProtocol.UDP);

                UDP l4 = new UDP();
                l4.setSourcePort(TransportPort.of(1));
                l4.setDestinationPort(TransportPort.of(9090));

                Data l7 = new Data();
                l7.setData(new byte[1000]);

                l2.setPayload(l3);
                l3.setPayload(l4);
                l4.setPayload(l7);

                byte[] serializedData = l2.serialize();

                OFPacketOut po = sw.getOFFactory().buildPacketOut() /* mySwitch is some IOFSwitch object */
                        .setData(serializedData)
                        .setActions(Collections.singletonList((OFAction) sw.getOFFactory().actions().output(OFPort.FLOOD, 0xffFFffFF)))
                        .setInPort(OFPort.CONTROLLER)
                        .build();

                sw.write(po);

                logger.warn("QUI");

                /* Retrieve the deserialized packet in message */
                Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

                /* Various getters and setters are exposed in Ethernet */
                MacAddress srcMac = eth.getSourceMACAddress();
                VlanVid vlanId = VlanVid.ofVlan(eth.getVlanID());

                /*
                 * Check the ethertype of the Ethernet frame and retrieve the appropriate payload.
                 * Note the shallow equality check. EthType caches and reuses instances for valid types.
                 */
                if (eth.getEtherType() == EthType.IPv4) {
                    /* We got an IPv4 packet; get the payload from Ethernet */
                    IPv4 ipv4 = (IPv4) eth.getPayload();

                    /* Various getters and setters are exposed in IPv4 */
                    byte[] ipOptions = ipv4.getOptions();
                    IPv4Address dstIp = ipv4.getDestinationAddress();

                    /* Still more to come... */

                    logger.info("Received a packet from " + ipv4.getSourceAddress() + "to " + ipv4.getDestinationAddress() +  ". Packet count: " + counter.incrementAndGet());
                    //logger.info("Packet protocol: " + ipv4.getProtocol().toString());

                } else if (eth.getEtherType() == EthType.ARP) {
                    /* We got an ARP packet; get the payload from Ethernet */
                    ARP arp = (ARP) eth.getPayload();

                    /* Various getters and setters are exposed in ARP */
                    boolean gratuitous = arp.isGratuitous();

                    logger.info("Got an ARP message");

                } else {
                    /* Unhandled ethertype */
                }
                break;
            default:
                break;
        }

        return Command.CONTINUE;
    }

    @Override
    public String getName() {
        return CounterModule.class.getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        return null;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        counter = new AtomicLong(0);
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        logger = LoggerFactory.getLogger(CounterModule.class);
        logger.info("Module init()");
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        logger.info("Module startUp()");
    }
}
