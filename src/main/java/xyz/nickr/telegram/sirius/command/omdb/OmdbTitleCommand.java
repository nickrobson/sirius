package xyz.nickr.telegram.sirius.command.omdb;

import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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
                    series = new Series(args[0]);
                }
            } catch (Exception ex) {
                message.getChat().sendMessage(SendableTextMessage.plain("That's not a valid series.").replyTo(message).build());
                return;
            }
            List<String> pages = new LinkedList<>();
            pages.add(
                    (series.getGenre().equals("N/A") ? "" : escape(series.getGenre() + " " + series.getType()) + "\n") +
                            (series.getRating().equals("N/A") || series.getVotes().equals("N/A") ? "" : "_Rating:_ " + escape(series.getRating() + " from " + series.getVotes() + " votes") + "\n") +
                            (series.getMetascore().equals("N/A") ? "" : "_Metascore:_ " + escape(series.getMetascore()) + "\n") +
                            (series.getRuntime().equals("N/A") ? "" : "_Runtime:_ " + escape(series.getRuntime()) + "\n") +
                            (series.getDirector().equals("N/A") ? "" : "_Director:_ " + escape(series.getDirector()) + "\n") +
                            (series.getWriter().equals("N/A") ? "" : "_Writer:_ " + escape(series.getWriter()) + "\n") +
                            (series.getActors().equals("N/A") ? "" : "_Actors:_ " + escape(series.getActors()) + "\n") +
                            (series.getAwards().equals("N/A") ? "" : "_Awards:_ " + escape(series.getAwards()) + "\n") +
                            (series.getLanguage().equals("N/A") ? "" : "_Language:_ " + escape(series.getLanguage()) + "\n") +
                            (series.getCountry().equals("N/A") ? "" : "_Country:_ " + escape(series.getCountry()))
            );
            if (!series.getPlot().equals("N/A")) {
                pages.add("_Plot:_ " + escape(series.getPlot()));
            }
            for (Season season : series.getSeasons()) {
                pages.addAll(getSummaryPages(season));
            }
            PaginatedData paginatedData = new PaginatedData(pages);
            paginatedData.setHeader("*" + escape(series.getName()) + "*" + escape(" (" + series.getYear() + "), ") + "[" + series.getImdbId() + "](http://www.imdb.com/title/" + series.getImdbId() + ")");
            paginatedData.setParseMode(ParseMode.MARKDOWN);
            paginatedData.send(0, message);
        }
    }

    private static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMM ''uu").withLocale(Locale.US);

    public static List<String> getSummaryPages(Season season) {
        List<String> seasonLines = new LinkedList<>();
        for (Episode episode : season.getEpisodes()) {
            List<String> episodeLines = new LinkedList<>();
            episodeLines.add(String.format("*S%sE%s*: _%s_", escape(season.getId()), escape(episode.getId()), escape(episode.getName())));
            if (!episode.getRating().equals("N/A")) {
                episodeLines.add(escape(episode.getRating() + "/10"));
            }
            if (episode.getReleaseDate() != null) {
                episodeLines.add(FORMATTER.format(episode.getReleaseDate()));
            }
            if (!episode.getImdbId().equals("N/A")) {
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
