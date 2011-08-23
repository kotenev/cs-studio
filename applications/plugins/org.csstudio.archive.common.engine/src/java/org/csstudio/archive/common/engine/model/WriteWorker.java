/*
 * Copyright (c) 2011 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.archive.common.engine.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.csstudio.archive.common.engine.service.IServiceProvider;
import org.csstudio.archive.common.service.ArchiveServiceException;
import org.csstudio.archive.common.service.IArchiveEngineFacade;
import org.csstudio.archive.common.service.sample.IArchiveSample;
import org.csstudio.domain.desy.calc.CumulativeAverageCache;
import org.csstudio.domain.desy.service.osgi.OsgiServiceUnavailableException;
import org.csstudio.domain.desy.system.ISystemVariable;
import org.csstudio.domain.desy.task.AbstractTimeMeasuredRunnable;
import org.csstudio.domain.desy.time.TimeInstant;
import org.csstudio.domain.desy.time.TimeInstant.TimeInstantBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * The independent worker to write/submit the samples to the persistence layer.
 *
 * @author bknerr
 * @since 14.02.2011
 */
final class WriteWorker extends AbstractTimeMeasuredRunnable {

    private static final Logger WORKER_LOG = LoggerFactory.getLogger(WriteWorker.class);
    /**
     * See configuration of this logger - if log4j is used - see log4j.properties
     */
    private static final Logger EMAIL_LOG =
        LoggerFactory.getLogger("ErrorPerEmailLogger");

    private final String _name;
    private final Collection<ArchiveChannel<Serializable, ISystemVariable<Serializable>>> _scalarChannels =
        Lists.newLinkedList();
    private final Collection<ArchiveChannel<Serializable, ISystemVariable<Serializable>>> _multiScalarChannels =
        Lists.newLinkedList();


    private final long _periodInMS;
    /** Average number of values per write run */
    private final CumulativeAverageCache _avgWriteCount = new CumulativeAverageCache();

    private final IServiceProvider _provider;

    private TimeInstant _lastWriteTime;

    /**
     * Constructor.
     */
    public WriteWorker(@Nonnull final IServiceProvider provider,
                       @Nonnull final String name,
                       @Nonnull final Collection<ArchiveChannel<Serializable, ISystemVariable<Serializable>>> channels,
                       final long periodInMS) {
        _provider = provider;
        _name = name;
        for (final ArchiveChannel<Serializable, ISystemVariable<Serializable>> channel : channels) {
            if (channel.isMultiScalar()) {
                _multiScalarChannels.add(channel);
            } else {
                _scalarChannels.add(channel);
            }
        }

        _periodInMS = periodInMS;

        WORKER_LOG.info("{} created with period {}ms", _name, periodInMS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void measuredRun() {
        try {
            //WORKER_LOG.info("WRITER RUN: {}", _name);

            List<IArchiveSample<Serializable, ISystemVariable<Serializable>>> samples = Collections.emptyList();

            // TODO (bknerr) : To make this distinction here is clearly a break of encapsulation and separation of concern
            // The split of the archive samples into scalar and multiscalar samples (of the same supertype) should only be
            // done in the service... take care of it when the frontend is replaced by pvmanager!
            samples = collectSamplesFromBuffers(_scalarChannels);
            long written = writeSamples(_provider,  samples);

            samples = collectSamplesFromBuffers(_multiScalarChannels);
            written += writeSamples(_provider,  samples);
            WORKER_LOG.info("WriteWorker: {}", written);

            _lastWriteTime = TimeInstantBuilder.fromNow();
            _avgWriteCount.accumulate(Double.valueOf(written));

        } catch (final ArchiveServiceException e) {
            WORKER_LOG.error("Exception within service impl. Data rescue should be handled there.", e);
        } catch (final Throwable t) {
            WORKER_LOG.error("Unknown throwable in thread {}.", _name);
            t.printStackTrace();
            EMAIL_LOG.info("Unknown throwable in thread {}. See event.log for more info.", _name);
        }
    }

    private long writeSamples(@Nonnull final IServiceProvider provider,
                              @Nonnull final List<IArchiveSample<Serializable, ISystemVariable<Serializable>>> samples) throws ArchiveServiceException {

        IArchiveEngineFacade service;
        try {
            service = provider.getEngineFacade();
        } catch (final OsgiServiceUnavailableException e) {
            EMAIL_LOG.error("Archive service unavailable: {}\nRescue serialized samples", e.getMessage());
            ArchiveEngineSampleRescuer.with(samples).rescue();
            return 0;
        }
        // when there's a service, the service impl handles the rescue of data
        service.writeSamples(samples);
        return samples.size();
    }


    @Nonnull
    private LinkedList<IArchiveSample<Serializable, ISystemVariable<Serializable>>>
    collectSamplesFromBuffers(@Nonnull final Collection<ArchiveChannel<Serializable, ISystemVariable<Serializable>>> channels) {

        final LinkedList<IArchiveSample<Serializable, ISystemVariable<Serializable>>> allSamples =
            Lists.newLinkedList();

        for (final ArchiveChannel<Serializable, ISystemVariable<Serializable>> channel : channels) {

            final SampleBuffer<Serializable, ISystemVariable<Serializable>, IArchiveSample<Serializable, ISystemVariable<Serializable>>> buffer =
                channel.getSampleBuffer();

            buffer.updateStats();
            buffer.drainTo(allSamples);
        }
        return allSamples;
    }

    public long getPeriodInMS() {
        return _periodInMS;
    }

    @Nonnull
    protected CumulativeAverageCache getAvgWriteCount() {
        return _avgWriteCount;
    }

    @CheckForNull
    protected TimeInstant getLastWriteTime() {
        return _lastWriteTime;
    }

    @Override
    public void clear() {
        super.clear();
        _avgWriteCount.clear();
        _lastWriteTime = null;
    }
}
