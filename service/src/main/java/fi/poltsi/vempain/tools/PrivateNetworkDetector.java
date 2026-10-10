package fi.poltsi.vempain.tools;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Finds the private networks this process is attached to (the Docker bridge or Swarm overlay networks it shares with the other
 * services), so that an API token can be restricted to the network the calling service really comes from. Only private ranges count:
 * IPv4 10/8, 172.16/12, 192.168/16 and IPv6 unique local addresses (fc00::/7). Loopback, link-local and public addresses are ignored.
 */
@Slf4j
public final class PrivateNetworkDetector {
	private PrivateNetworkDetector() {
	}

	public record PrivateNetwork(String network, String interfaceName, String address) {
	}

	/**
	 * The private networks of every interface that is up, IPv4 first, in interface order.
	 */
	public static List<PrivateNetwork> detect() {
		try {
			var interfaces = NetworkInterface.getNetworkInterfaces();
			if (interfaces == null) {
				return List.of();
			}
			var found = new ArrayList<PrivateNetwork>();
			for (var networkInterface : Collections.list(interfaces)) {
				if (!networkInterface.isUp() || networkInterface.isLoopback()) {
					continue;
				}
				for (var interfaceAddress : networkInterface.getInterfaceAddresses()) {
					var candidate = describe(networkInterface.getName(), interfaceAddress.getAddress(), interfaceAddress.getNetworkPrefixLength());
					if (candidate != null) {
						found.add(candidate);
					}
				}
			}
			found.sort((a, b) -> Boolean.compare(a.address()
			                                      .contains(":"), b.address()
			                                                       .contains(":")));
			return found;
		} catch (SocketException e) {
			log.warn("Could not enumerate the network interfaces: {}", e.getMessage());
			return List.of();
		}
	}

	/**
	 * The network of one interface address, or null when the address is not private (or the prefix is unusable).
	 */
	static PrivateNetwork describe(String interfaceName, InetAddress address, int prefixLength) {
		if (address == null || !isPrivate(address)) {
			return null;
		}
		var bytes = address.getAddress();
		var bits = bytes.length * 8;
		if (prefixLength <= 0 || prefixLength > bits) {
			return null;
		}
		var masked = bytes.clone();
		for (int i = 0; i < bits; i++) {
			if (i >= prefixLength) {
				masked[i / 8] &= (byte) ~(1 << (7 - i % 8));
			}
		}
		try {
			var network = InetAddress.getByAddress(masked);
			return new PrivateNetwork(NetworkMatcher.normalize(NetworkMatcher.canonical(network) + "/" + prefixLength), interfaceName,
									  NetworkMatcher.canonical(address));
		} catch (java.net.UnknownHostException | IllegalArgumentException e) {
			return null;
		}
	}

	static boolean isPrivate(InetAddress address) {
		if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress() || address.isMulticastAddress()) {
			return false;
		}
		if (address instanceof Inet4Address) {
			return address.isSiteLocalAddress();
		}
		if (address instanceof Inet6Address) {
			// Unique local addresses fc00::/7
			return (address.getAddress()[0] & 0xFE) == 0xFC;
		}
		return false;
	}
}
