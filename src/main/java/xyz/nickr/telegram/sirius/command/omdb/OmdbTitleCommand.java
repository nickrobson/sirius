package xyz.nickr.telegram.sirius.command.omdb;

import java.text.Collator;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
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
import xyz.nickr.telepad.util.Partition;

/**
 * @author Nick Robson
 */
public class OmdbTitleCommand extends Command {

    public OmdbTitleCommand() {
        super("omdbtitle");
        this.setHelp("gets omdbapi.com's information about a show");
        this.setUsage("[name/imdb id]");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length != 1) {
            sendUsage(message);
        } else {
            Series series;
            try {
                series = Sirius.getSeriesController().getSeriesByLink(args[0]);
                if (series == null) {
                    series = Sirius.getSeriesController().getSeries(args[0], false);
                }
                if (series == null) {
                    series = new Series(args[0], false);
                }
            } catch (Exception ex) {
                message.getChat().sendMessage(SendableTextMessage.plain("That's not a valid series.").replyTo(message).build());
                return;
            }
            Collator collator = bot.getCollator();
            List<String> pages = new LinkedList<>();
            pages.add(
                    (collator.equals("N/A", series.getGenre()) ? "" : escape(series.getGenre() + " " + series.getType()) + "\n") +
                            (collator.equals("N/A", series.getRating()) || collator.equals("N/A", series.getVotes()) ? "" : "_Rating:_ " + escape(series.getRating() + " from " + series.getVotes() + " votes") + "\n") +
                            (collator.equals("N/A", series.getMetascore()) ? "" : "_Metascore:_ " + escape(series.getMetascore()) + "\n") +
                            (collator.equals("N/A", series.getRuntime()) ? "" : "_Runtime:_ " + escape(series.getRuntime()) + "\n") +
                            (collator.equals("N/A", series.getDirector()) ? "" : "_Director:_ " + escape(series.getDirector()) + "\n") +
                            (collator.equals("N/A", series.getWriter()) ? "" : "_Writer:_ " + escape(series.getWriter()) + "\n") +
                            (collator.equals("N/A", series.getActors()) ? "" : "_Actors:_ " + escape(series.getActors()) + "\n") +
                            (collator.equals("N/A", series.getAwards()) ? "" : "_Awards:_ " + escape(series.getAwards()) + "\n") +
                            (collator.equals("N/A", series.getLanguage()) ? "" : "_Language:_ " + escape(series.getLanguage()) + "\n") +
                            (collator.equals("N/A", series.getCountry()) ? "" : "_Country:_ " + escape(series.getCountry()))
            );
            if (!collator.equals("N/A", series.getPlot())) {
                pages.add("_Plot:_ " + escape(series.getPlot()));
            }
            for (Season season : series.getSeasons()) {
                pages.addAll(getSummaryPages(bot, season));
            }
            PaginatedData paginatedData = new PaginatedData(pages);
            paginatedData.setHeader("*" + escape(series.getName()) + "*" + escape(" (" + series.getYear() + "), ") + "[" + series.getImdbId() + "](http://www.imdb.com/title/" + series.getImdbId() + ")");
            paginatedData.setParseMode(ParseMode.MARKDOWN);
            paginatedData.send(0, message);
        }
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMM ''uu");

    public static List<String> getSummaryPages(TelepadBot bot, Season season) {
        DateTimeFormatter formatter = FORMATTER.withLocale(bot.getLocale());
        List<String> seasonLines = new LinkedList<>();
        for (Episode episode : season.getEpisodes()) {
            List<String> episodeLines = new LinkedList<>();
            episodeLines.add(String.format("*S%sE%s*: _%s_", escape(season.getId()), escape(episode.getId()), escape(episode.getName())));
            if (!bot.getCollator().equals("N/A", episode.getRating())) {
                episodeLines.add(escape(episode.getRating() + "/10"));
            }
            if (episode.getReleaseDate() != null) {
                episodeLines.add(formatter.format(episode.getReleaseDate()));
            }
            if (!bot.getCollator().equals("N/A", episode.getImdbId())) {
                episodeLines.add(String.format("[link](http://www.imdb.com/title/%s)", episode.getImdbId()));
            }
            seasonLines.add(String.join(", ", episodeLines));
        }
        return Partition.partition(seasonLines, 10)
                .stream()
                .map(s -> "_Season " + season.getId() + "_\n" + String.join("\n", s))
                .collect(Collectors.toList());
    }

}
