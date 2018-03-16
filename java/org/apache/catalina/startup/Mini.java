package org.apache.catalina.startup;

import org.apache.tomcat.util.buf.Ascii;

import edu.gmu.swe.knarr.runtime.Symbolicator;

public class Mini {

	public static void main(String[] args) {
		byte tainted[] = new byte[2];

		tainted[0] = Symbolicator.symbolic((byte)'a');
		tainted[1] = Symbolicator.symbolic((byte)'a');

		if (tainted[0] == (byte)'z')
			return;

		if (tainted[0] == Ascii.toLower((byte)'X'))
			return;

		if (Ascii.toLower(tainted[0]) == Ascii.toLower((byte)'A'))
			tainted[1] = (byte)'b';
		else
			tainted[1] = (byte)'c';

		System.out.println(Symbolicator.dumpConstraints());


	}

}
