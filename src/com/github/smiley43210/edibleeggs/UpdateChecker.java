package com.github.smiley43210.edibleeggs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

	private final String CHANGELOG_URL = "https://raw.githubusercontent.com/Smiley43210/EdibleEggs/master/Changelog.txt";
	private final String CHANGELOG_REGEX = "v([0-9A-Za-z]+(?:\\.[0-9A-Za-z]+)*)";

	@SuppressWarnings("unused")
	private Main plugin;

	private String newVersion;
	private boolean hasUpdate = false;

	public UpdateChecker(Main plugin) throws UpdateCheckException {
		this.plugin = plugin;

		try {
			URL changelogURL = new URL(CHANGELOG_URL);
			BufferedReader changelogReader = new BufferedReader(new InputStreamReader(changelogURL.openStream()));
			String latestVersion = changelogReader.readLine();

			Pattern pattern = Pattern.compile(CHANGELOG_REGEX);
			Matcher matcher = pattern.matcher(latestVersion);

			if (matcher.find()) {
				newVersion = matcher.group(1);
				hasUpdate = !plugin.getDescription().getVersion().equals(matcher.group(1));
			} else {
				throw new UpdateCheckException();
			}
		} catch (MalformedURLException e) {
			throw new UpdateCheckException();
		} catch (IOException e) {
			throw new UpdateCheckException();
		}
	}

	public boolean hasUpdate() {
		return hasUpdate;
	}

	public String getNewVersion() {
		return newVersion;
	}

}
