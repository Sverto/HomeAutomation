/*-
 * #%L
 * Home Automation
 * %%
 * Copyright (C) 2016 - 2017 Koen Serneels
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package be.error.rpi.dac.dimmer.builder;

import static be.error.rpi.config.RunConfig.getInstance;
import static be.error.rpi.dac.dimmer.builder.DimDirection.UP;
import static tuwien.auto.calimero.process.ProcessCommunicationBase.SCALING;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.process.ProcessEvent;

/**
 * @author Koen Serneels
 */
public class KnxDimmerProcessListener extends AbstractDimmerProcessListener {

	protected static final Logger logger = LoggerFactory.getLogger(KnxDimmerProcessListener.class);

	private final GroupAddress onOff;
	private final Optional<GroupAddress> precenseDetectorLock;
	private final Optional<GroupAddress> onOffOverride;
	private final Optional<GroupAddress> dim;
	private final Optional<GroupAddress> dimAbsolute;
	private final Optional<GroupAddress> dimAbsoluteOverride;
	private final Optional<GroupAddress> deactivateOnOtherUse;

	private int minDimVal = 1;

	private boolean suspendedForOtherUse;

	KnxDimmerProcessListener(final GroupAddress onOff, final Optional<GroupAddress> onOffOverride, final Optional<GroupAddress> precenseDetectorLock,
			final Optional<GroupAddress> dim, final Optional<GroupAddress> dimAbsolute, final Optional<GroupAddress> dimAbsoluteOverride,
			final Optional<Integer> minDimVal, final Optional<GroupAddress> deactivateOnOtherUse, Dimmer dimmer) {
		this.onOff = onOff;
		this.dim = dim;
		this.dimAbsolute = dimAbsolute;
		this.onOffOverride = onOffOverride;
		this.dimAbsoluteOverride = dimAbsoluteOverride;
		this.precenseDetectorLock = precenseDetectorLock;
		this.deactivateOnOtherUse = deactivateOnOtherUse;
		if (minDimVal.isPresent()) {
			this.minDimVal = minDimVal.get();
		}

		setDimmer(dimmer);
	}

	@Override
	public void groupWrite(final ProcessEvent e) {
		try {

			if (deactivateOnOtherUse.isPresent() && e.getDestination().equals(deactivateOnOtherUse.get())) {
				this.suspendedForOtherUse = asBool(e);
			}

			if (suspendedForOtherUse) {
				dimmer.interrupt();
				return;
			}

			if (dim.isPresent() && e.getDestination().equals(dim.get())) {
				dimmer.interrupt();
				boolean b = asBool(e);

				if (b) {
					DimmerCommand dimmerCommand = new DimmerCommand(dimmer.getLastDimDirection() == UP ? new BigDecimal(minDimVal) : new BigDecimal(100),
							e.getSourceAddr());
					if (onOffOverride.isPresent()) {
						getInstance().getKnxConnectionFactory().runWithProcessCommunicator(pc -> pc.write(precenseDetectorLock.get(), true));
						dimmerCommand.setOverride();
					}
					dimmer.putCommand(dimmerCommand);
				}
			}

			if (dimAbsolute.isPresent() && e.getDestination().equals(dimAbsolute.get())) {
				dimmer.interrupt();
				int i = asUnsigned(e, SCALING);
				DimmerCommand dimmerCommand = new DimmerCommand(new BigDecimal(i), e.getSourceAddr());
				dimmer.putCommand(dimmerCommand);
			}

			if (dimAbsoluteOverride.isPresent() && e.getDestination().equals(dimAbsoluteOverride.get())) {
				dimmer.interrupt();
				int i = asUnsigned(e, SCALING);
				DimmerCommand dimmerCommand = new DimmerCommand(new BigDecimal(i), e.getSourceAddr());
				if (i > 0) {
					getInstance().getKnxConnectionFactory().runWithProcessCommunicator(pc -> pc.write(precenseDetectorLock.get(), true));
					dimmerCommand.setOverride();
				} else {
					getInstance().getKnxConnectionFactory().runWithProcessCommunicator(pc -> pc.write(precenseDetectorLock.get(), false));
				}
				dimmer.putCommand(dimmerCommand);
			}

			if (e.getDestination().equals(onOff)) {
				if (dimmer.getLastDimCommand().isPresent() && dimmer.getLastDimCommand().get().isOverride()) {
					return;
				}
				dimmer.interrupt();
				boolean b = asBool(e);
				DimmerCommand dimmerCommand = new DimmerCommand(b ? new BigDecimal(100) : new BigDecimal(0), e.getSourceAddr());
				dimmer.putCommand(dimmerCommand);
			}

			if (onOffOverride.isPresent() && e.getDestination().equals(onOffOverride.get())) {
				dimmer.interrupt();
				boolean b = asBool(e);
				DimmerCommand dimmerCommand = new DimmerCommand(b ? new BigDecimal(100) : new BigDecimal(0), e.getSourceAddr());
				if (b) {
					getInstance().getKnxConnectionFactory().runWithProcessCommunicator(pc -> pc.write(precenseDetectorLock.get(), true));
					dimmerCommand.setOverride();
				} else {
					getInstance().getKnxConnectionFactory().runWithProcessCommunicator(pc -> pc.write(precenseDetectorLock.get(), false));
				}
				dimmer.putCommand(dimmerCommand);
			}
		} catch (Exception ex) {
			logger.error("Could not process KNX dim command", ex);
		}
	}

	@Override
	public void groupReadRequest(final ProcessEvent e) {

	}

	@Override
	public void groupReadResponse(final ProcessEvent e) {

	}

	@Override
	public void detached(final DetachEvent e) {

	}

	public boolean isDimmerGroupAddress(GroupAddress groupAddress) {
		if (onOff.equals(groupAddress)) {
			return true;
		}

		if (dim.isPresent() && dim.get().equals(groupAddress)) {
			return true;
		}

		if (dimAbsolute.isPresent() && dim.get().equals(groupAddress)) {
			return true;
		}

		return false;
	}

	public int getMinDimVal() {
		return minDimVal;
	}
}
