package com.ctp.squiddler.inkpack;

import java.io.File;
import java.util.List;

import lombok.Data;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages")
@Data
public class ApplicationParameters {
	@Parameter(names = { "-?", "--help" }, help = true)
	private boolean help;

	@Parameter(descriptionKey = "source.folders", required = true)
	private List<String> sourceFolders;
	
	@Parameter(names = {"-t", "--target"}, descriptionKey = "target.folder", required = true)
	private File target;
}
