package xyz.nickr.telegram.sirius.command.tv;

import java.util.AbstractMap;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telegram.sirius.tv.Episode;
import xyz.nickr.telegram.sirius.tv.Season;
import xyz.nickr.telegram.sirius.tv.Series;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;
import xyz.nickr.telepad.util.PaginatedData;

/**
 * @author Nick Robson
 */
public class WhoCommand extends Command {

    public WhoCommand() {
        super("who");
        this.setHelp("gets your progress on all series");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        Map<String, String> progress = Sirius.getProgressController().getProgress(message.getSender());

        List<String> lines = new LinkedList<>();
        for (Map.Entry<String, String> entry : progress.entrySet()) {
            Series series = Sirius.getSeriesController().getSeries(entry.getKey(), true);
            int oldSize = lines.size();
            try {
                Calendar now = Calendar.getInstance();

                Function<Season, Optional<Episode>> latestEpisodeFunc =
                        s -> IntStream.range(0, s.getEpisodes().length)
                                .mapToObj(i -> s.getEpisodes()[s.getEpisodes().length - i - 1])
                                .map(e -> new AbstractMap.SimpleEntry<>(e, e.getReleaseDate()))
                                .filter(e -> e != null && e.getValue() != null && e.getValue().before(now))
                                .map(AbstractMap.SimpleEntry::getKey)
                                .findFirst();

                Map.Entry<Season, Episode> latestEpisode = IntStream.range(0, series.getSeasons().length)
                        .mapToObj(i -> series.getSeasons()[series.getSeasons().length - i - 1])
                        .filter(Objects::nonNull)
                        .filter(s -> s.getEpisodes().length > 0)
                        .map(s -> {
                            Optional<Episode> opt = latestEpisodeFunc.apply(s);
                            return opt.map(e -> new AbstractMap.SimpleEntry<>(s, e)).orElse(null);
                        })
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

                if (latestEpisode != null) {
                    String[] episodeParts = entry.getValue().substring(1).split("E");

                    int seasonId = Integer.parseInt(episodeParts[0]);
                    int episodeId = Integer.parseInt(episodeParts[1]);

                    int newSeasonId = Integer.parseInt(latestEpisode.getKey().getId());

                    boolean added = false;
                    if (newSeasonId > seasonId) {
                        added = true;
                    } else if (newSeasonId == seasonId) {
                        Episode latestEp = latestEpisode.getValue();
                        if (Integer.parseInt(latestEp.getId()) > episodeId) {
                            added = true;
                        }
                    }

                    System.out.println("latest for " + series.getName() + ": S" + latestEpisode.getKey().getId() + "E" + latestEpisode.getValue().getId());

                    if (added) {
                        lines.add(String.format("*%s*: %s _(latest: S%sE%s)_", escape(series.getName(), false), escape(entry.getValue()), escape(latestEpisode.getKey().getId(), false), escape(latestEpisode.getValue().getId(), false)));
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (lines.size() == oldSize)
                lines.add(String.format("*%s*: %s", escape(series.getName(), false), escape(entry.getValue())));
        }
        lines.sort(String.CASE_INSENSITIVE_ORDER);

        PaginatedData paginatedData = new PaginatedData(lines, 15);
        paginatedData.setParseMode(ParseMode.MARKDOWN);
        paginatedData.send(0, message);
    }

}
