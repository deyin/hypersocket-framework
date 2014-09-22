package com.hypersocket.properties;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hypersocket.attributes.Attribute;
import com.hypersocket.attributes.AttributeCategory;
import com.hypersocket.resource.AbstractResource;

public abstract class ResourceTemplateRepositoryImpl extends PropertyRepositoryImpl implements ResourceTemplateRepository {

	static Logger log = LoggerFactory.getLogger(ResourceTemplateRepositoryImpl.class);

	DatabasePropertyStore configPropertyStore;

	Map<String, PropertyCategory> activeCategories = new HashMap<String, PropertyCategory>();	
	List<PropertyTemplate> activeTemplates = new ArrayList<PropertyTemplate>();
	Set<String> propertyNames = new HashSet<String>();
	
	String resourceXmlPath;
	
	protected ResourcePropertyStore getPropertyStore() {
		return configPropertyStore;
	}
	
	@Override
	public Set<String> getPropertyNames() {
		return propertyNames;
	}
	
	public void loadPropertyTemplates(String resourceXmlPath) {
		
		this.resourceXmlPath = resourceXmlPath;
		configPropertyStore = new DatabasePropertyStore(this);
		
		String context = null;
		try {
			Enumeration<URL> urls = getClass().getClassLoader().getResources(
					resourceXmlPath);
			if(!urls.hasMoreElements()) {
				throw new IllegalArgumentException(resourceXmlPath + " does not exist!");
			}
			
			
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				try {
					context = loadPropertyTemplates(url);
				} catch (Exception e) {
					log.error("Failed to process " + url.toExternalForm(), e);
				}
			}
			
			
		} catch (IOException e) {
			log.error("Failed to load propertyTemplate.xml resources", e);
		}
		
		
		if(context!=null) {
			loadAttributeTemplates(context);
		}

	}

	private String loadPropertyTemplates(URL url) throws SAXException,
			IOException, ParserConfigurationException {

		DocumentBuilderFactory xmlFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder xmlBuilder = xmlFactory.newDocumentBuilder();
		Document doc = xmlBuilder.parse(url.openStream());

		Element root = doc.getDocumentElement();
		String context = null;
		
		if(root.hasAttribute("extends")) {
			String extendsTemplates = root.getAttribute("extends");
			StringTokenizer t = new StringTokenizer(extendsTemplates, ",");
			while(t.hasMoreTokens()) {
				Enumeration<URL> extendUrls = getClass().getClassLoader().getResources(t.nextToken());
				while(extendUrls.hasMoreElements()) {
					URL extendUrl = extendUrls.nextElement();
					try {
						context = loadPropertyTemplates(extendUrl);
					} catch(Exception e) {
						log.error("Failed to process " + extendUrl.toExternalForm(), e);
					}
				}
			}
		} else if(root.hasAttribute("context")) {
			context = root.getAttribute("context");
		} 
		
		if(log.isInfoEnabled()) {
			log.info("Loading property template resource " + url.toExternalForm());
		}
		
		loadPropertyCategories(doc);
		
		return context;
	}
	
	private void loadAttributeTemplates(String context) {
		
		
		for(AttributeCategory c : getAttributeCategories(context)) {
			
			PropertyCategory cat = registerPropertyCategory(
					"attributeCategory" + String.valueOf(c.getId()),
					"UserAttributes",
					c.getWeight(),
					true,
					context,
					"");
			
			for(Attribute attr : c.getAttributes()) {
				registerPropertyItem(
						cat,
						configPropertyStore,
						"attribute" + String.valueOf(attr.getId()),
						attr.generateMetaData(),
						"",
						attr.getWeight(),
						attr.getHidden(),
						attr.getReadOnly(),
						attr.getDefaultValue());
			}
		}
		
		
		
	}

	private void loadPropertyCategories(Document doc) throws IOException {

		NodeList list = doc.getElementsByTagName("propertyCategory");

		for (int i = 0; i < list.getLength(); i++) {
			Element node = (Element) list.item(i);

			if (!node.hasAttribute("resourceKey")
					|| !node.hasAttribute("resourceBundle")
					|| !node.hasAttribute("weight")) {
				throw new IOException(
						"<propertyCategory> requires resourceKey, resourceBundle and weight attributes");
			}

			if (log.isInfoEnabled()) {
				log.info("Registering category "
						+ node.getAttribute("resourceKey") + " for bundle " + node.getAttribute("resourceBundle"));
			}

			String group = "system";
			if(node.hasAttribute("group")) {
				group = node.getAttribute("group");
			}
			
			PropertyCategory cat = registerPropertyCategory(
					node.getAttribute("resourceKey"),
					node.getAttribute("resourceBundle"),
					Integer.parseInt(node.getAttribute("weight")),
					false,
					group,
					node.getAttribute("displayMode"));

			NodeList properties = node.getElementsByTagName("property");

			for (int x = 0; x < properties.getLength(); x++) {

				Element pnode = (Element) properties.item(x);

				if (pnode.hasAttribute("store")) {
					throw new IOException("store attribute not supported for resource templates!");
				}

				try {
					registerPropertyItem(
							cat,
							getPropertyStore(),
							pnode.getAttribute("resourceKey"),
							generateMetaData(pnode),
							pnode.getAttribute("mapping"),
							Integer.parseInt(pnode.getAttribute("weight")),
							pnode.hasAttribute("hidden")
									&& pnode.getAttribute("hidden")
											.equalsIgnoreCase("true"),
							pnode.hasAttribute("readOnly")
									&& pnode.getAttribute("readOnly")
											.equalsIgnoreCase("true"),
									Boolean.getBoolean("hypersocket.development") && pnode.hasAttribute("developmentValue") ? pnode.getAttribute("developmentValue") : pnode.getAttribute("defaultValue"));
				} catch (Throwable e) {
					log.error("Failed to register property item", e);
				}
			}
		}
	}

	private String generateMetaData(Element pnode) {
		NamedNodeMap map = pnode.getAttributes();
		StringBuffer buf = new StringBuffer();
		buf.append("{");
		for (int i = 0; i < map.getLength(); i++) {
			Node n = map.item(i);
			if (buf.length() > 1) {
				buf.append(", ");
			}
			buf.append("\"");
			buf.append(n.getNodeName());
			buf.append("\": ");
			if(n.getNodeName().equals("options")) {
				buf.append("[");
				StringTokenizer t = new StringTokenizer(n.getNodeValue(), ",");
				while(t.hasMoreTokens()) {
					String opt = t.nextToken();
					buf.append("{ \"name\": \"");
					buf.append(opt);
					buf.append("\", \"value\": \"");
					buf.append(opt);
					buf.append("\"}");
					if(t.hasMoreTokens()) {
						buf.append(",");
					}
				}
				buf.append("]");
			} else {
				buf.append("\"");
				buf.append(n.getNodeValue());
				buf.append("\"");
			}
		}
		buf.append("}");
		return buf.toString();
	}

	
	private void registerPropertyItem(PropertyCategory category,
			PropertyStore propertyStore, String resourceKey, String metaData,
			String mapping, int weight, boolean hidden, boolean readOnly, String defaultValue) {

		if(log.isInfoEnabled()) {
			log.info("Registering property " + resourceKey);
		}
		
		if(defaultValue.startsWith("classpath:")) {
				String url = defaultValue.substring(10);
				InputStream in = getClass().getResourceAsStream(url);
				if(in!=null) {
					try {
						defaultValue = IOUtils.toString(in);
					} catch (IOException e) {
						log.error("Failed to load default value classpath resource " + defaultValue, e);
					}
				} else {
					log.error("Failed to load default value classpath resource " + url);
				}
		}
		
		propertyNames.add(resourceKey);
		
		PropertyTemplate template = propertyStore.getPropertyTemplate(resourceKey);
		if (template == null) {
			template = new PropertyTemplate();
			template.setResourceKey(resourceKey);
		}
		
		template.setMetaData(metaData);
		template.setDefaultValue(defaultValue);
		template.setWeight(weight);
		template.setHidden(hidden);
		template.setReadOnly(readOnly);
		template.setMapping(mapping);
		template.setCategory(category);
		template.setPropertyStore(propertyStore);
		
		propertyStore.registerTemplate(template, resourceXmlPath);
		category.getTemplates().add(template);
		activeTemplates.add(template);
		
		Collections.sort(category.getTemplates(), new Comparator<AbstractPropertyTemplate>() {
			@Override
			public int compare(AbstractPropertyTemplate cat1, AbstractPropertyTemplate cat2) {
				return cat1.getWeight().compareTo(cat2.getWeight());
			}
		});
		
		Collections.sort(activeTemplates, new Comparator<AbstractPropertyTemplate>() {
			@Override
			public int compare(AbstractPropertyTemplate t1, AbstractPropertyTemplate t2) {
				return t1.getResourceKey().compareTo(t2.getResourceKey());
			}
		});
		
	}

	private PropertyCategory registerPropertyCategory(String resourceKey,
			String bundle, int weight, boolean userCreated, String group, String displayMode) {

		if (activeCategories.containsKey(resourceKey)) {
			throw new IllegalStateException(
						"Cannot register "
								+ resourceKey
								+ "/"
								+ bundle
								+ " as the resource key is already registered by bundle "
								+ activeCategories.get(resourceKey).getBundle());
		}
		
		if(activeCategories.containsKey(resourceKey)) {
			return activeCategories.get(resourceKey);
		}
		
		PropertyCategory category = new PropertyCategory();
		category.setBundle(bundle);
		category.setCategoryKey(resourceKey);
		category.setCategoryGroup(group);
		category.setDisplayMode(displayMode);
		category.setWeight(weight);
		category.setUserCreated(userCreated);
		
		activeCategories.put(category.getCategoryKey(), category);
		return category;
	}

	@Override
	public String getValue(AbstractResource resource, String resourceKey) {

		PropertyTemplate template = getPropertyStore().getPropertyTemplate(resourceKey);

		if (template == null) {
			throw new IllegalStateException(resourceKey
					+ " is not a registered configuration item");
		}

		return getPropertyStore().getPropertyValue(template, resource);
	}

	@Override
	public Integer getIntValue(AbstractResource resource, String name) throws NumberFormatException {
		return Integer.parseInt(getValue(resource, name));
	}

	@Override
	public Boolean getBooleanValue(AbstractResource resource, String name) {
		return Boolean.parseBoolean(getValue(resource, name));
	}

	@Override
	public void setValue(AbstractResource resource, String resourceKey, String value) {

		AbstractPropertyTemplate template = getPropertyStore().getPropertyTemplate(resourceKey);

		if (template == null) {
			throw new IllegalStateException(resourceKey
					+ " is not a registered configuration item");
		}

		if(template.isReadOnly()) {
			return;
		}
		
		getPropertyStore().setPropertyValue(template, resource, value);
	}

	@Override
	public void setValue(AbstractResource resource, String resourceKey, Integer value)  {
		setValue(resource, resourceKey, String.valueOf(value));
	}

	@Override
	public void setValue(AbstractResource resource, String name, Boolean value) {
		setValue(resource, name, String.valueOf(value));
	}


	@Override
	public Collection<PropertyCategory> getPropertyCategories(AbstractResource resource)  {
		
		List<PropertyCategory> cats = new ArrayList<PropertyCategory>();
		for(PropertyCategory c : activeCategories.values()) {
			PropertyCategory tmp = new PropertyCategory();
			tmp.setBundle(c.getBundle());
			tmp.setCategoryKey(c.getCategoryKey());
			tmp.setWeight(c.getWeight());
			tmp.setDisplayMode(c.getDisplayMode());
			tmp.setUserCreated(c.isUserCreated());
			for(AbstractPropertyTemplate t : c.getTemplates()) {
				tmp.getTemplates().add(new ResourcePropertyTemplate(t, resource, getPropertyStore())); 
			}
			cats.add(tmp);
		}
		
		Collections.sort(cats, new Comparator<PropertyCategory>() {
			@Override
			public int compare(PropertyCategory cat1, PropertyCategory cat2) {
				return cat1.getWeight().compareTo(cat2.getWeight());
			}
		});
		
		return Collections.unmodifiableCollection(cats);
	}
	
	@Override
	public Collection<PropertyCategory> getPropertyCategories(AbstractResource resource, String group) {

		List<PropertyCategory> cats = new ArrayList<PropertyCategory>();
		for(PropertyCategory c : activeCategories.values()) {
			if(!c.getCategoryGroup().equals(group)) {
				continue;
			}
			PropertyCategory tmp = new PropertyCategory();
			tmp.setBundle(c.getBundle());
			tmp.setCategoryKey(c.getCategoryKey());
			tmp.setWeight(c.getWeight());
			tmp.setUserCreated(c.isUserCreated());
			tmp.setDisplayMode(c.getDisplayMode());
			for(AbstractPropertyTemplate t : c.getTemplates()) {
				tmp.getTemplates().add(new ResourcePropertyTemplate(t, resource, getPropertyStore())); 
			}
			cats.add(tmp);
		}
		
		Collections.sort(cats, new Comparator<PropertyCategory>() {
			@Override
			public int compare(PropertyCategory cat1, PropertyCategory cat2) {
				return cat1.getWeight().compareTo(cat2.getWeight());
			}
		});
		
		return Collections.unmodifiableCollection(cats);
	}
	
	@Override
	public Collection<PropertyTemplate> getPropertyTemplates() {
		return Collections.unmodifiableCollection(activeTemplates);
	}

	@Override
	public String[] explodeValues(String values) {
		StringTokenizer t = new StringTokenizer(values, "]|[");
		List<String> ret = new ArrayList<String>();
		while (t.hasMoreTokens()) {
			ret.add(t.nextToken());
		}
		return ret.toArray(new String[0]);
	}
	
	@Override
	public String[] getValues(AbstractResource resource, String name) {

		String values = getValue(resource, name);
		return explodeValues(values);
	}
	
	@Override
	public Map<String,String> getProperties(AbstractResource resource) {
		
		Map<String,String> properties = new HashMap<String,String>();
		for(String name : getPropertyNames()) {
			properties.put(name, getValue(resource, name));
		}
		return properties;
	}
}
