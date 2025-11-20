package com.example.demo.service;

import com.example.demo.model.PodcastEpisode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Service
public class SoundOnRssService {

    private final RestTemplate restTemplate;

    public SoundOnRssService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 從 SoundOn RSS 取得最近幾集節目資訊
     */
    public List<PodcastEpisode> fetchEpisodes(String rssUrl, int limit) {
        ResponseEntity<String> resp = restTemplate.getForEntity(rssUrl, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            return Collections.emptyList();
        }

        String xml = resp.getBody();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList itemNodes = doc.getElementsByTagName("item");
            int count = Math.min(itemNodes.getLength(), limit);

            List<PodcastEpisode> result = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                Element item = (Element) itemNodes.item(i);

                String title = getTagText(item, "title");
                String link = getTagText(item, "link");
                String guid = getTagText(item, "guid");
                String pubDate = getTagText(item, "pubDate");

                String audioUrl = null;
                NodeList enclosures = item.getElementsByTagName("enclosure");
                if (enclosures.getLength() > 0) {
                    Element enclosure = (Element) enclosures.item(0);
                    audioUrl = enclosure.getAttribute("url");
                }

                PodcastEpisode ep = new PodcastEpisode();
                ep.setTitle(title);
                ep.setLink(link);
                ep.setGuid(guid);
                ep.setPubDate(pubDate);
                ep.setAudioUrl(audioUrl);

                result.add(ep);
            }

            return result;
        } catch (Exception e) {
            // 可以加上 log，看錯在哪裡
            return Collections.emptyList();
        }
    }

    private String getTagText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

}
