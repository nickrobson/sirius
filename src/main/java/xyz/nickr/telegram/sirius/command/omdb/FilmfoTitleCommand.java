package xyz.nickr.telegram.sirius.command.omdb;

import java.text.Collator;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
public class FilmfoTitleCommand extends Command {

    public FilmfoTitleCommand() {
        super("title");
        this.setHelp("gets filmfo information about a show");
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
            List<String> pages = new ArrayList<>();
            pages.add(
                    (escape(String.join(", ", series.getGenres()) + " " + series.getType()) + "\n") +
                            ("_Rating:_ " + escape(series.getRating() + " from " + series.getRatingCount() + " votes") + "\n") +
                            ("_Runtime:_ " + escape(String.valueOf(series.getRuntime())) + "m\n") +
                            ("_Director:_ " + escape(String.join(", ", series.getDirectors())) + "\n") +
                            ("_Writer:_ " + escape(String.join(", ", series.getCreators())) + "\n") +
                            ("_Actors:_ " + escape(String.join(", ", series.getActors())) + "\n") +
                            ("_Awards:_ " + escape(series.getAwards()) + "\n") +
                            ("_Languages:_ " + escape(String.join(", ", series.getLanguages())) + "\n") +
                            ("_Countries:_ " + escape(String.join(", ", series.getCountries())))
            );
            if (!collator.equals("N/A", series.getPlot())) {
                pages.add("_Plot:_ " + escape(series.getPlot()));
            }
            for (Season season : series.getSeasons()) {
                pages.addAll(getSummaryPages(bot, season));
            }
            PaginatedData paginatedData = new PaginatedData(pages);
            paginatedData.setHeader("*" + escape(series.getName()) + "*, [" + series.getImdbId() + "](http://www.imdb.com/title/" + series.getImdbId() + ")");
            paginatedData.setParseMode(ParseMode.MARKDOWN);
            paginatedData.send(0, message);
        }
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMM ''uu");

    public static List<String> getSummaryPages(TelepadBot bot, Season season) {
        List<String> seasonLines = new ArrayList<>();
        if (season == null)
            return seasonLines;
        DateTimeFormatter formatter = FORMATTER.withLocale(bot.getLocale());
        for (Episode episode : season.getEpisodes()) {
            List<String> episodeLines = new ArrayList<>();
            episodeLines.add(String.format("*S%sE%s*: _%s_", escape(String.valueOf(season.getId())), escape(String.valueOf(episode.getId())), escape(episode.getName())));
            if (episode.getReleaseDate() != null) {
                episodeLines.add(formatter.format(episode.getReleaseDate()));
            }
            episodeLines.add(String.format("[link](http://www.imdb.com/title/%s)", episode.getImdbId()));
            seasonLines.add(String.join(", ", episodeLines));
        }
        return Partition.partition(seasonLines, 10)
                .stream()
                .map(s -> "_Season " + season.getId() + "_\n" + String.join("\n", s))
                .collect(Collectors.toList());
    }

}
