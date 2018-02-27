package pw.lemmmy.kristpay.config;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {
	private ConfigurationLoader<CommentedConfigurationNode> loader;
	private Path path;
	private ConfigurationNode rootNode;
	
	public ConfigLoader(ConfigurationLoader<CommentedConfigurationNode> loader, Path path) {
		this.loader = loader;
		this.path = path;
	}
	
	public Config loadConfig() throws IOException, ObjectMappingException {
		Config config;
		
		if (!Files.exists(path)) {
			config = new Config();
			rootNode = loader.createEmptyNode(loader.getDefaultOptions());
			rootNode.setValue(TypeToken.of(Config.class), config);
			loader.save(rootNode);
		} else {
			rootNode = loader.load();
			config = rootNode.getValue(TypeToken.of(Config.class));
		}
		
		return config;
	}
}
