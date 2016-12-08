package be.error.rpi.dac.dimmer.config.scenes;

import static be.error.rpi.config.RunConfig.getInstance;
import static be.error.rpi.dac.dimmer.builder.SceneContext.active;
import static be.error.rpi.dac.dimmer.builder.SceneContext.inactive;
import static tuwien.auto.calimero.process.ProcessCommunicationBase.UNSCALED;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessEvent;

import be.error.rpi.dac.dimmer.builder.Dimmer;
import be.error.rpi.dac.dimmer.builder.DimmerCommand;
import be.error.rpi.knx.ProcessListenerAdapter;

/**
 * Created by koen on 04.10.16.
 */
public class GvComfort implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(GvComfort.class);

	private Dimmer eethoek;
	private Dimmer zithoek;
	private Dimmer keuken;

	private boolean active;

	private ProcessCommunicator pc;

	public GvComfort(final Dimmer eethoek, final Dimmer zithoek, final Dimmer keuken) {
		this.eethoek = eethoek;
		this.zithoek = zithoek;
		this.keuken = keuken;
		this.pc = getInstance().getKnxConnectionFactory().createProcessCommunicator();
	}

	@Override
	public void run() {
		ProcessListenerAdapter processListenerAdapter = new ProcessListenerAdapter() {
			@Override
			public void groupWrite(final ProcessEvent e) {
				try {
					if (e.getDestination().equals(new GroupAddress("11/0/0"))) {
						active = !active;
						int i = asUnsigned(e, UNSCALED);
						if (i == 1) {
							sceneOne(active, e);
						}
					}
				} catch (Exception ex) {
					logger.error(GvComfort.class.getSimpleName() + " had exception in ProcessListenerAdapter", ex);
				}

				super.groupWrite(e);
			}
		};
		getInstance().getKnxConnectionFactory().createProcessCommunicator(processListenerAdapter);
	}

	private void sceneOne(boolean on, ProcessEvent e) throws Exception {

		if (on) {
			pc.write(new GroupAddress("11/0/1"), true);
		} else {
			pc.write(new GroupAddress("11/0/1"), false);
		}

		eethoek.interrupt();
		zithoek.interrupt();
		keuken.interrupt();
		keuken.putCommand(new DimmerCommand(new BigDecimal(on ? 1 : 0), e.getSourceAddr(), on ? active(new GroupAddress("5/0/2")) : inactive()));
		zithoek.putCommand(new DimmerCommand(new BigDecimal(on ? 1 : 0), e.getSourceAddr(), on ? active(new GroupAddress("5/3/1")) : inactive()));
		eethoek.putCommand(new DimmerCommand(new BigDecimal(on ? 1 : 0), e.getSourceAddr(), on ? active(new GroupAddress("5/1/0")) : inactive()));
	}
}
