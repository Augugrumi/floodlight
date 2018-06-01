package net.floodlightcontroller.counter;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
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

                    logger.info("Received a packet from " + dstIp.toString() + ". Packet count: " + counter.incrementAndGet());

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
