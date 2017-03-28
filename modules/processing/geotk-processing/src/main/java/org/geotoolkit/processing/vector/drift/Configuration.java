package org.geotoolkit.processing.vector.drift;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.util.CharSequences;
import org.geotoolkit.process.ProcessException;

final class Configuration {
    /**
     * The directory of the configuration file.
     * Also the root directory where to search for data.
     */
    final Path directory;

    final DriftPredictor.Weight[] weights;

    /**
     * Maximum number of trajectories to track;
     */
    final int maximumTrajectoryCount;

    Configuration(final Path directory) throws IOException, ProcessException {
        this.directory = directory;
        int count = 2_000_000;       // Default value.
        final List<DriftPredictor.Weight> wg = new ArrayList<>();
        for (String line : Files.readAllLines(directory.resolve("config.txt"))) {
            if (!(line = line.trim()).isEmpty() && !line.startsWith("#")) {
                final int s = line.indexOf('=');
                if (s < 0) {
                    throw new ProcessException("Not a key-value pair: " + line, null);
                }
                final String keyword = line.substring(0, s).trim();
                final CharSequence[] values = CharSequences.split(line.substring(s+1).trim(), ',');
                switch (keyword.toLowerCase()) {
                    case "weights": {
                        if (values.length != 3) {
                            throw new ProcessException(keyword + " shall have exactly three values.", null);
                        }
                        wg.add(new DriftPredictor.Weight(
                                Double.parseDouble(values[0].toString()),
                                Double.parseDouble(values[1].toString()),
                                Double.parseDouble(values[2].toString())));
                        break;
                    }
                    case "maximum_trajectory_count": {
                        if (values.length != 1) {
                            throw new ProcessException(keyword + " shall have exactly one value.", null);
                        }
                        count = Integer.parseInt(values[0].toString());
                        break;
                    }
                    default: {
                        throw new ProcessException("Unknown property: " + keyword, null);
                    }
                }
            }
        }
        weights = wg.toArray(new DriftPredictor.Weight[wg.size()]);
        maximumTrajectoryCount = count;
    }
}
