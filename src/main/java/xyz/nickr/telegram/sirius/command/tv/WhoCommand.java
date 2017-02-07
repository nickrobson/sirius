package xyz.nickr.telegram.sirius.command.tv;

import java.time.LocalDateTime;
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
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
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
        this.setUsage("(user)");
        this.setHelp("gets your (or someone's) progress on all series");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        long userId = message.getSender().getId();
        if (args.length > 0) {
            Long id = bot.getUserCache().getUserId(args[0]);
            if (id == null) {
                message.getChat().sendMessage(SendableTextMessage.plain("No user found matching that name.").replyTo(message).build());
                return;
            }
            userId = id;
        }
        Map<String, String> progress = Sirius.getProgressController().getProgress(userId);

        List<String> lines = new LinkedList<>();
        for (Map.Entry<String, String> entry : progress.entrySet()) {
            Series series = Sirius.getSeriesController().getSeries(entry.getKey(), true);
            int oldSize = lines.size();
            try {
                LocalDateTime now = LocalDateTime.now();

                Function<Season, Optional<Episode>> latestEpisodeFunc =
                        s -> IntStream.range(0, s.getEpisodes().length)
                                .mapToObj(i -> s.getEpisodes()[s.getEpisodes().length - i - 1])
                                .map(e -> new AbstractMap.SimpleEntry<>(e, e.getReleaseDate()))
                                .filter(e -> e != null && e.getValue() != null && e.getValue().isBefore(now) && !e.getKey().getRating().equals("N/A"))
                                .map(Map.Entry::getKey)
                                .findFirst();

                Map.Entry<Season, Episode> latestEpisode = IntStream.range(0, series.getSeasons().length)
                        .mapToObj(i -> series.getSeasons()[series.getSeasons().length - i - 1])
                        .filter(Objects::nonNull)
                        .filter(s -> s.getEpisodes().length > 0)
                        .map(s -> latestEpisodeFunc.apply(s)
                                .map(e -> new AbstractMap.SimpleEntry<>(s, e))
                                .orElse(null))
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
