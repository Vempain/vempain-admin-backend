package fi.poltsi.vempain.tools;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * IPv4/IPv6 network membership for the API token network restriction. A network is written in CIDR notation; a plain address is the
 * single-host network (/32 for IPv4, /128 for IPv6). Only address literals are accepted, so no name resolution ever happens.
 */
public final class NetworkMatcher {
	private static final Pattern IPV4 = Pattern.compile("^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
	private static final Pattern IPV6 = Pattern.compile("^[0-9A-Fa-f:.]+$");

	private NetworkMatcher() {
	}

	/**
	 * Validates the network and returns it in canonical form ({@code address/prefix}).
	 *
	 * @throws IllegalArgumentException when the text is not an IPv4/IPv6 address or CIDR network
	 */
	public static String normalize(String network) {
		var parsed = parse(network);
		return canonical(parsed.address) + "/" + parsed.prefixLength;
	}

	/**
	 * Address text as RFC 5952 writes it: IPv4 dotted quad, IPv6 lower-case hex groups with the longest run of zero groups compressed.
	 */
	public static String canonical(InetAddress address) {
		var bytes = address.getAddress();
		if (bytes.length == 4) {
			return address.getHostAddress();
		}
		var groups = new int[8];
		for (int i = 0; i < 8; i++) {
			groups[i] = ((bytes[2 * i] & 0xFF) << 8) | (bytes[2 * i + 1] & 0xFF);
		}
		int bestStart = -1;
		int bestLength = 0;
		for (int i = 0; i < 8; i++) {
			if (groups[i] != 0) {
				continue;
			}
			int j = i;
			while (j < 8 && groups[j] == 0) {
				j++;
			}
			if (j - i > bestLength) {
				bestStart = i;
				bestLength = j - i;
			}
			i = j;
		}
		if (bestLength < 2) {
			bestStart = -1;
		}
		var text = new StringBuilder();
		for (int i = 0; i < 8; i++) {
			if (i == bestStart) {
				text.append("::");
				i += bestLength - 1;
				continue;
			}
			if (!text.isEmpty() && text.charAt(text.length() - 1) != ':') {
				text.append(':');
			}
			text.append(Integer.toHexString(groups[i]));
		}
		return text.toString();
	}

	/**
	 * Whether {@code clientAddress} (an address literal) lies in {@code network}. Any malformed input yields {@code false}.
	 */
	public static boolean matches(String network, String clientAddress) {
		if (clientAddress == null || clientAddress.isBlank()) {
			return false;
		}
		try {
			var parsed = parse(network);
			var client = parseAddress(stripZone(clientAddress.trim()));
			var networkBytes = parsed.address.getAddress();
			var clientBytes = client.getAddress();

			if (networkBytes.length != clientBytes.length) {
				return false;
			}

			int fullBytes = parsed.prefixLength / 8;
			for (int i = 0; i < fullBytes; i++) {
				if (networkBytes[i] != clientBytes[i]) {
					return false;
				}
			}
			int remaining = parsed.prefixLength % 8;
			if (remaining == 0) {
				return true;
			}
			int mask = (0xFF << (8 - remaining)) & 0xFF;
			return (networkBytes[fullBytes] & mask) == (clientBytes[fullBytes] & mask);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static String stripZone(String address) {
		var zone = address.indexOf('%');
		return zone > 0 ? address.substring(0, zone) : address;
	}

	private static ParsedNetwork parse(String network) {
		if (network == null || network.isBlank()) {
			throw new IllegalArgumentException("Network is empty");
		}
		var text = network.trim();
		var slash = text.indexOf('/');
		var addressText = slash < 0 ? text : text.substring(0, slash);
		var address = parseAddress(addressText);
		var maxPrefix = address.getAddress().length * 8;
		int prefix;

		if (slash < 0) {
			prefix = maxPrefix;
		} else {
			try {
				prefix = Integer.parseInt(text.substring(slash + 1));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid prefix length in network " + network);
			}
			if (prefix < 0 || prefix > maxPrefix) {
				throw new IllegalArgumentException("Prefix length must be between 0 and " + maxPrefix + " in network " + network);
			}
		}

		return new ParsedNetwork(address, prefix);
	}

	private static InetAddress parseAddress(String text) {
		var isIpv4 = IPV4.matcher(text)
		                 .matches();
		var isIpv6 = !isIpv4 && text.contains(":") && IPV6.matcher(text)
		                                                  .matches();
		if (!isIpv4 && !isIpv6) {
			throw new IllegalArgumentException("Not an IPv4 or IPv6 address literal: " + text);
		}
		try {
			// Only literals reach this point, so getByName never resolves a name
			return InetAddress.getByName(text);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Not an IPv4 or IPv6 address literal: " + text);
		}
	}

	private record ParsedNetwork(InetAddress address, int prefixLength) {
	}
}
