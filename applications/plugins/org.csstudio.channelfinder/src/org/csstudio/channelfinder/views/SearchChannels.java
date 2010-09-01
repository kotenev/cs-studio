package org.csstudio.channelfinder.views;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import gov.bnl.channelfinder.api.ChannelFinderClient;
import gov.bnl.channelfinder.model.XmlChannel;
import gov.bnl.channelfinder.model.XmlChannels;
import gov.bnl.channelfinder.model.XmlProperty;
import gov.bnl.channelfinder.model.XmlTag;

import org.csstudio.channelfinder.Activator;
import org.csstudio.channelfinder.preferences.PreferenceConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.sun.jersey.core.util.MultivaluedMapImpl;

public class SearchChannels extends Job {
	private String searchPattern;
	private ChannelFinderView channelFinderView;

	public SearchChannels(String name, String pattern,
			ChannelFinderView channelFinderView) {
		super(name);
		this.searchPattern = pattern;
		this.channelFinderView = channelFinderView;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Seaching channels ", IProgressMonitor.UNKNOWN);
		IPreferencesService prefs = Platform.getPreferencesService();
		System.out.println(prefs.getString(Activator.PLUGIN_ID,
				PreferenceConstants.ChannelFinder_URL, "defaultURL", null));
		buildSearchMap(searchPattern);
		// final XmlChannels channels = client.getInstance().queryChannelsName(
		// searchPattern);
		final XmlChannels channels;
		try {
			channels = sort(ChannelFinderClient.getInstance().queryChannels(
					buildSearchMap1(searchPattern)));
			// final XmlChannels channels = testData(); // to use test data
			//TODO incorrect call
			// replace with PlatformUI.getWorkbench().getDisplay().asyncExec(task);
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					// return set of channels sorted by name
					// each channel has its properties and tags sorted by name.
					channelFinderView.updateList(channels);
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	private static MultivaluedMapImpl buildSearchMap(String searchPattern) {
		MultivaluedMapImpl map = new MultivaluedMapImpl();
		String[] words = searchPattern.split("\\s");
		if (words.length < 0) {
			// ERROR
		}
		for (int index = 0; index < words.length; index++) {
			if (!words[index].contains("=")) {
				// this is a name value
				map.add("~name", words[index]);
			} else {
				// this is a property or tag
				String key = words[index].split("=")[0];
				String[] values = words[index].split("=")[1].split(",");
				if (key.equalsIgnoreCase("Tags")) {
					for (int i = 0; i < values.length; i++)
						map.add("~tag", values[i]);
				} else {
					for (int i = 0; i < values.length; i++)
						map.add(key, values[i]);
				}
			}
		}
		return map;
	}

	private static Map<String, String> buildSearchMap1(String searchPattern) {
		Hashtable<String, String> map = new Hashtable<String, String>();
		String[] words = searchPattern.split("\\s");
		if (words.length < 0) {
			// ERROR
		}
		for (int index = 0; index < words.length; index++) {
			if (!words[index].contains("=")) {
				// this is a name value
				map.put("~name", words[index]);
			} else {
				// this is a property or tag
				String key = words[index].split("=")[0];
				String[] values = words[index].split("=")[1].split(",");
				if (key.equals("Tags")) {
					for (int i = 0; i < values.length; i++)
						map.put("~tag", values[i]);
				} else {
					for (int i = 0; i < values.length; i++)
						map.put(key, values[i]);
				}
			}
		}
		return map;
	}

	private static XmlChannels paddedChannels(XmlChannels channels) {
		XmlChannels chs = null;
		// sorted with no duplicates
		Collection<String> allProperties = new TreeSet<String>();
		Collection<String> allTags = new TreeSet<String>();
		// union of all the properties/tags of all channels.
		Iterator<XmlChannel> itr = channels.getChannels().iterator();
		while (itr.hasNext()) {
			XmlChannel element = itr.next();
			for (XmlProperty property : element.getXmlProperties()) {
				allProperties.add(property.getName());
			}
			for (XmlTag tag : element.getXmlTags()) {
				allTags.add(tag.getName());
			}
		}
		// second iteration to pad all the channels
		Iterator<XmlChannel> paditr = channels.getChannels().iterator();
		while (paditr.hasNext()) {
			XmlChannel element = itr.next();
			// XmlProperty prop = new TreeSet();
		}
		return chs;
	}

	private static XmlChannel sort(XmlChannel channel) {
		XmlChannel ch = new XmlChannel(channel.getName(), channel.getOwner());
		Collection<XmlProperty> sortedProps = new TreeSet<XmlProperty>(
				new XmlPropertyComparator());
		sortedProps.addAll(channel.getXmlProperties());
		ch.setXmlProperties(sortedProps);
		Collection<XmlTag> sortedTags = new TreeSet<XmlTag>(
				new XmlTagComparator());
		sortedTags.addAll(channel.getXmlTags());
		ch.setXmlTags(sortedTags);
		return ch;
	}

	private static XmlChannels sort(XmlChannels channels) {
		XmlChannels chs = new XmlChannels();
		Collection<XmlChannel> sortedChannels = new TreeSet<XmlChannel>(
				new XmlChannelComparator());
		Iterator<XmlChannel> itr = channels.getChannels().iterator();
		while (itr.hasNext()) {
			sortedChannels.add(sort(itr.next()));
		}
		chs.setChannels(sortedChannels);
		return chs;
	}

	public static XmlChannels testData() {
		XmlChannels channels = new XmlChannels();
		channels
				.addChannel(getchannel("fff", "apple", "aProp", "aVal", "aTag"));
		channels.addChannel(getchannel("bbb", "ball", "bProp", "bVal", "bTag"));
		XmlChannel ch = getchannel("ccc", "egg", "cProp", "cVal", "cTag");
		ch.addProperty(new XmlProperty("aProp", "egg", "aVal"));
		ch.addProperty(new XmlProperty("bProp", "egg", "bVal"));
		ch.addTag(new XmlTag("aTag", "kunal"));
		channels.addChannel(ch);
		channels.addChannel(getchannel("ddd", "dag", "dProp", "dVal", "dTag"));
		channels.addChannel(getchannel("eee", "cat", "dProp", "dVal", "eTag"));
		return channels;
	}

	private static XmlChannel getchannel(String name, String owner,
			String prop, String val, String tag) {
		XmlChannel ch = new XmlChannel(name, owner);
		ch.addProperty(new XmlProperty(prop, owner, val));
		ch.addTag(new XmlTag(tag, owner));
		return ch;
	}

}
